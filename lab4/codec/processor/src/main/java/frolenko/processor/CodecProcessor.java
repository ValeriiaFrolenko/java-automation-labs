package frolenko.processor;

import com.squareup.javapoet.*;
import frolenko.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("frolenko.annotations.Packet")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class CodecProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Messager messager;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Packet.class)) {
            TypeElement packetElement = (TypeElement) element;
            if (validate(packetElement)) {
                try {
                    generateCodecInterface(packetElement);
                } catch (IOException e) {
                    error(packetElement, "Failed to generate codec interface: " + e.getMessage());
                }
            }
        }
        return true;
    }

    private boolean validate(TypeElement packetElement) {
        boolean valid = true;

        List<VariableElement> fields = ElementFilter.fieldsIn(packetElement.getEnclosedElements());

        for (VariableElement field : fields) {
            if (field.getAnnotation(Field.class) != null) {
                if (!isPublic(field)) {
                    error(field, "@Field must be public");
                    valid = false;
                }
                if (!isAllowedFieldType(field.asType())) {
                    error(field, "@Field must be one of: byte, short, int, long");
                    valid = false;
                }
            }
        }

        List<VariableElement> messageFields = fields.stream()
                .filter(f -> f.getAnnotation(MessageField.class) != null)
                .toList();

        if (messageFields.size() > 1) {
            error(packetElement, "@Packet class must have at most one @MessageField");
            valid = false;
        }

        if (messageFields.size() == 1) {
            TypeElement messageElement = (TypeElement) typeUtils.asElement(messageFields.get(0).asType());
            if (messageElement.getAnnotation(MessageDef.class) == null) {
                error(messageFields.get(0), "@MessageField must point to a class annotated with @MessageDef");
                valid = false;
            } else {
                valid = validateMessageDef(messageElement) && valid;
            }
        }

        return valid;
    }

    private boolean validateMessageDef(TypeElement messageElement) {
        boolean valid = true;

        List<VariableElement> fields = ElementFilter.fieldsIn(messageElement.getEnclosedElements());

        for (VariableElement field : fields) {
            if (field.getAnnotation(Field.class) != null) {
                if (!isPublic(field)) {
                    error(field, "@Field must be public");
                    valid = false;
                }
                if (!isAllowedFieldType(field.asType())) {
                    error(field, "@Field must be one of: byte, short, int, long");
                    valid = false;
                }
            }
        }

        List<VariableElement> payloadFields = fields.stream()
                .filter(f -> f.getAnnotation(Payload.class) != null)
                .toList();

        if (payloadFields.size() > 1) {
            error(messageElement, "@MessageDef class must have at most one @Payload");
            valid = false;
        }

        for (VariableElement payloadField : payloadFields) {
            if (!isPublic(payloadField)) {
                error(payloadField, "@Payload must be public");
                valid = false;
            }
            if (!isAllowedPayloadType(payloadField.asType())) {
                error(payloadField, "@Payload must be String or byte[]");
                valid = false;
            }
        }

        return valid;
    }

    private boolean isPublic(VariableElement field) {
        return field.getModifiers().contains(Modifier.PUBLIC);
    }

    private boolean isAllowedFieldType(TypeMirror type) {
        return switch (type.getKind()) {
            case BYTE, SHORT, INT, LONG -> true;
            default -> false;
        };
    }

    private boolean isAllowedPayloadType(TypeMirror type) {
        TypeMirror stringType = elementUtils.getTypeElement("java.lang.String").asType();
        if (typeUtils.isSameType(type, stringType)) return true;

        TypeMirror byteArray = typeUtils.getArrayType(typeUtils.getPrimitiveType(TypeKind.BYTE));
        return typeUtils.isSameType(type, byteArray);
    }

    private void generateCodecInterface(TypeElement packetElement) throws IOException {
        String modelName = packetElement.getSimpleName().toString();
        String interfaceName = modelName + "Codec";
        String packageName = elementUtils.getPackageOf(packetElement).getQualifiedName().toString();

        ClassName modelClass = ClassName.get(packetElement);
        ClassName generatedCodec = ClassName.get("frolenko.annotations", "GeneratedCodec");

        AnnotationSpec annotation = AnnotationSpec.builder(generatedCodec)
                .addMember("modelClass", "$T.class", modelClass)
                .build();

        MethodSpec encode = MethodSpec.methodBuilder("encode")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(byte[].class)
                .addParameter(modelClass, "packet")
                .addParameter(byte[].class, "key")
                .build();

        MethodSpec decode = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(modelClass)
                .addParameter(byte[].class, "data")
                .addParameter(byte[].class, "key")
                .build();

        TypeSpec interfaceSpec = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(annotation)
                .addMethod(encode)
                .addMethod(decode)
                .build();

        JavaFile.builder(packageName, interfaceSpec)
                .build()
                .writeTo(processingEnv.getFiler());
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}