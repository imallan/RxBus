package com.imallan.rxbus.processor;

import com.google.auto.service.AutoService;
import com.imallan.rxbus.annotation.Subscribe;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SuppressWarnings("unused")
public class BusProcessor extends AbstractProcessor {

    private Filer mFiler;
    private Messager mMessager;
    private boolean isFirstRound = true;
    private Map<Element, ArrayList<Element>> mMap = new LinkedHashMap<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Subscribe.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {

        if (!isFirstRound) {
            return false;
        }

        isFirstRound = false;

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Subscribe.class);
        for (Element element : elements) {
            if (element.getKind() == ElementKind.METHOD) {
                Element enclosingClass = element.getEnclosingElement();
                ArrayList<Element> list = mMap.get(enclosingClass);
                if (list == null) {
                    list = new ArrayList<>();
                    mMap.put(enclosingClass, list);
                }
                list.add(element);
            }
        }

        Set<Element> classSet = mMap.keySet();
        ClassName busName = ClassName.bestGuess("com.imallan.rxbus.Bus");
        for (Element classElement : classSet) {
            TypeSpec.Builder builder = TypeSpec.classBuilder(classElement.getSimpleName() + "$BindUtils");
            builder.addAnnotation(
                    AnnotationSpec.builder(ClassName.bestGuess("java.lang.SuppressWarnings"))
                            .addMember("value", "$S", "unused").build()
            );

            MethodSpec.Builder unbindMethodBuilder = MethodSpec.methodBuilder("unbind")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(
                            busName, "bus").build()
                    )
                    .addParameter(ParameterSpec.builder(
                            ClassName.get(classElement.asType()), "target", Modifier.FINAL).build()
                    )
                    .returns(TypeName.VOID);
            unbindMethodBuilder.addStatement("bus.unbind(target)");

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(
                            busName, "bus").build()
                    )
                    .addParameter(ParameterSpec.builder(
                            ClassName.get(classElement.asType()), "target", Modifier.FINAL).build()
                    )
                    .returns(TypeName.VOID);
            methodBuilder.addStatement("bus.unbind(target)");
            for (Element methodElement : mMap.get(classElement)) {
                if (methodElement instanceof ExecutableElement) {
                    ExecutableElement exeElement = (ExecutableElement) methodElement;
                    List<? extends VariableElement> parameters = exeElement.getParameters();
                    if (parameters.size() != 1) {
                        break;
                    }

                    VariableElement variableElement = parameters.get(0);
                    String className = ClassName.get(variableElement.asType()).toString();
                    ClassName clazz = ClassName.bestGuess(className);
                    ClassName action1Name = ClassName.bestGuess("rx.functions.Action1");
                    ClassName schedulerName = ClassName.bestGuess("rx.Scheduler");
                    TypeSpec action1 = TypeSpec.anonymousClassBuilder("")
                            .addSuperinterface(action1Name)
                            .addMethod(
                                    MethodSpec.methodBuilder("call")
                                            .addAnnotation(Override.class)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(Object.class, "o")
                                            .addStatement(
                                                    "$T ev = ($T) o", clazz, clazz
                                            )
                                            .addStatement(
                                                    "target.$L(ev)", methodElement.getSimpleName()
                                            )
                                            .returns(TypeName.VOID)
                                            .build()
                            )
                            .build();
                    Subscribe annotation = methodElement.getAnnotation(Subscribe.class);
                    methodBuilder.addStatement("bus.subscribe(target, $T.class, $L, $L)",
                            clazz, action1, annotation.scheduler());
                }
            }
            builder.addMethod(methodBuilder.build());
            builder.addMethod(unbindMethodBuilder.build());
            TypeSpec typeSpec = builder.addModifiers(Modifier.PUBLIC).build();
            JavaFile javaFile = JavaFile.builder(classElement.getEnclosingElement().toString(), typeSpec).build();
            try {
                javaFile.writeTo(mFiler);
            } catch (IOException e) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                return false;
            }
        }

        return true;
    }

}
