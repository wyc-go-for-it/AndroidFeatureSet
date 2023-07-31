package com.wyc.lib_myprocessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.wyc.label.lib_annotation.Printer")
@AutoService(Processor.class)
public class PrinterProcessor extends AbstractProcessor {
    private Filer mFiler;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        mFiler = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (TypeElement annotationType : annotations){
            final Set<? extends Element> hashSet = roundEnvironment.getElementsAnnotatedWith(annotationType);
            final List<TypeElement> typeElementList = new ArrayList<>();
            for (Element element : hashSet){
                ElementKind ik = element.getKind();
                if (ik == ElementKind.CLASS){
                    TypeElement typeElement = (TypeElement) element;
                    checkNoError(typeElement);
                    typeElementList.add(typeElement);
                }
            }
            writeJavaClassPot(typeElementList);
            return true;
        }
        return false;
    }

    private void writeJavaClass(final String packageNae,final List<TypeElement> typeElementList){
        BufferedWriter writer = null;
        try {

            messager.printMessage(Diagnostic.Kind.WARNING,"label printer list:" + Arrays.toString(typeElementList.toArray()));
            final JavaFileObject sourceFile = mFiler.createSourceFile( packageNae + ".LabelPrinterRegister");
            writer = new BufferedWriter(sourceFile.openWriter());

            writer.write("package " + packageNae + ";\n");

            for (TypeElement typeElement : typeElementList){
                writer.write("import " + typeElement.getQualifiedName() + ";");
            }

            writer.write("\n");

            writer.write("public class LabelPrinterRegister {\n");

            writer.write("public static void register(){\n");

            for (TypeElement typeElement : typeElementList){
                writer.write("LabelApp.registerClass(" + typeElement.getSimpleName() +".class );\n");
            }

            writer.write("}\n");

            writer.write("}\n");
        }catch (IOException e){
            messager.printMessage(Diagnostic.Kind.ERROR, "create register class error:" + e);
        }finally {
            if (writer != null){
                try {
                    writer.close();
                }catch (IOException ignore){
                }
            }
        }
    }

    private void writeJavaClassPot(final List<TypeElement> typeElementList){
        try {
            messager.printMessage(Diagnostic.Kind.WARNING,"label printer list:" + Arrays.toString(typeElementList.toArray()));

            final CodeBlock.Builder builder = CodeBlock.builder();
            for (TypeElement typeElement : typeElementList){
                builder.addStatement("LabelApp.registerClass($T.class )",ClassName.get(getPackageElement(typeElement).toString(), typeElement.getSimpleName().toString()));
            }

            final JavaFile javaFile = JavaFile.builder("com.wyc.label", TypeSpec.classBuilder("LabelPrinterRegister")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(MethodSpec.methodBuilder("register")
                            .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                            .addCode(builder.build())
                            .build()

                    ).build()

            ).build();

            javaFile.writeTo(mFiler);
        }catch (IOException e){
            messager.printMessage(Diagnostic.Kind.ERROR, "create register class error:" + e);
        }
    }

    private PackageElement getPackageElement(TypeElement subscriberClass) {
        Element candidate = subscriberClass.getEnclosingElement();
        while (!(candidate instanceof PackageElement)) {
            candidate = candidate.getEnclosingElement();
        }
        return (PackageElement) candidate;
    }

    private void checkNoError(TypeElement element){
        boolean hasSuperClass = false;

        final TypeMirror superclass = element.getSuperclass();
        final String name = superclass.toString();
        messager.printMessage(Diagnostic.Kind.WARNING,element.getSimpleName() + " extend " + name);
        if ("com.wyc.label.printer.AbstractPrinter".equals(superclass.toString())){
            hasSuperClass = true;
        }
        if (!hasSuperClass){
            final List<? extends TypeMirror> interfacesList = element.getInterfaces();
            for (TypeMirror interfaces : interfacesList){
                messager.printMessage(Diagnostic.Kind.WARNING,"implement interface" + "---" + name);
                if ("com.wyc.label.printer.IType".equals(interfaces.toString())){
                    hasSuperClass = true;
                    break;
                }
            }
            if (!hasSuperClass){
                messager.printMessage(Diagnostic.Kind.ERROR, "Printer class must implement com.wyc.label.printer.IType", element);
            }
        }
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Printer class must be public", element);
        }
    }

}