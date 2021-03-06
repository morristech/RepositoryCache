/*******************************************************************************
 * Copyright (c) 2016 Francisco Gonzalez-Armijo Riádigos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.kuassivi.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;

import com.kuassivi.annotation.RepositoryCache;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static java.util.Collections.singleton;
import static javax.lang.model.SourceVersion.latestSupported;

@AutoService(Processor.class)
public class RepositoryCacheProcessor extends AbstractProcessor {

    private Types    typeUtils;
    private Elements elementUtils;
    private Filer    filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Utils.initialize(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return singleton(RepositoryCache.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        try {

            Map<String, ProxyClassGenerator> proxyClassGeneratorMap =
                    new LinkedHashMap<String, ProxyClassGenerator>();

            // Scan classes
            for (Element annotatedElement : roundEnv
                    .getElementsAnnotatedWith(RepositoryCache.class)) {

                // Check if a method has been annotated with @RepositoryCache
                if (annotatedElement.getKind() != ElementKind.METHOD) {
                    throw new ProcessingException(annotatedElement,
                                                  "Only methods can be annotated with @%s",
                                                  RepositoryCache.class.getSimpleName());
                }

                if (!SuperficialValidation.validateElement(annotatedElement))
                    continue;

                // Wrap the Element
                AnnotatedMethod annotatedMethod = new AnnotatedMethod(annotatedElement);
                String qualifiedClassName = annotatedMethod.getQualifiedClassName();

                /*
                 * Required rules:
                 *
                 * Accessible method.
                 * Interfaces are allowed.
                 */
                Set<String> errors = com.kuassivi.compiler.AnnotationValidator
                        .with(annotatedElement, RepositoryCache.class)

                        .isAccessible(ElementKind.METHOD)
                        .isInterfaceAllowed(ElementKind.METHOD, true)

                        .getErrors();

                // Checks for errors
                if (!errors.isEmpty()) {
                    Utils.errors(annotatedElement, errors);
                    return true;
                }

                // Generate Proxy Classes
                ProxyClassGenerator generatorClass = proxyClassGeneratorMap.get(qualifiedClassName);
                if (generatorClass == null) {
                    generatorClass = new ProxyClassGenerator(elementUtils, qualifiedClassName);
                    proxyClassGeneratorMap.put(qualifiedClassName, generatorClass);
                    Utils.note("Processing class " + qualifiedClassName);
                }

                // Checks for Conflicts
                generatorClass.add(annotatedMethod);
            }

            // Generate File Code only once
            for (ProxyClassGenerator generatorClass : proxyClassGeneratorMap.values()) {
                generatorClass.generateCode(filer);
            }

        } catch (ProcessingException e) {
            Utils.error(e.getElement(), e.getMessage());
        } catch (IOException e) {
            Utils.error(null, e.getMessage());
        }

        return true;
    }
}
