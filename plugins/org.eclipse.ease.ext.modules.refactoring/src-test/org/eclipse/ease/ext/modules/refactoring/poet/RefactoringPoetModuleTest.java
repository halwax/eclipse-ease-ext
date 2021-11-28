
package org.eclipse.ease.ext.modules.refactoring.poet;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.ease.ext.modules.refactoring.poet.RefactoringPoetModule;
import org.junit.jupiter.api.Test;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

public class RefactoringPoetModuleTest {

	@Test
	public void test() {

		RefactoringPoetModule module = new RefactoringPoetModule();

		String packageName = "test";

		ClassName testClassName = module.poetClassName(packageName, "A");
		ClassName constantClassName = module.poetClassName(packageName, "Constant");

		AnnotationSpec testAnnotationSpec = module
				.poetAnnotation(module.poetSimpleClassName("org.junit.jupiter.api.Test"), it -> {
					it.addMember("test", "$T.$N", constantClassName, "NAME");
				});

		CodeBlock helloWorldCodeBlock = module.poetCodeBlock(it -> {
			it.addStatement("$T.out.println(\"$L\")", System.class, "Hello World");
		});

		MethodSpec methodSpec = module.poetMethod("test", it -> {
			it.addModifiers(module.poetModifiers("public", "static", "final"));
			it.addAnnotation(testAnnotationSpec);
			it.addCode(helloWorldCodeBlock);
		});

		TypeSpec testClass = module.poetClass(testClassName, it -> {
			it.addModifiers(module.poetModifiers("public"));
			it.addMethod(methodSpec);
		});

		JavaFile javaFile = module.poetJavaFile(packageName, testClass, it -> {
			it.skipJavaLangImports(true);
		});

		assertThat(javaFile.toString()).isNull();
	}

}
