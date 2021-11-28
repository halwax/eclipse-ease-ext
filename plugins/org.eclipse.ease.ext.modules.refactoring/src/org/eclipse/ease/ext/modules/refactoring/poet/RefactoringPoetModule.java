package org.eclipse.ease.ext.modules.refactoring.poet;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.JavaFile.Builder;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class RefactoringPoetModule extends AbstractScriptModule {

	@WrapToScript
	public JavaFile poetJavaFile(String packageName, TypeSpec typeSpec,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<JavaFile.Builder> builderConsumer) {
		Builder builder = JavaFile.builder(packageName, typeSpec);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public TypeSpec poetClass(ClassName name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<TypeSpec.Builder> builderConsumer) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public TypeSpec poetInterface(ClassName name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<TypeSpec.Builder> builderConsumer) {
		TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public TypeSpec poetEnum(ClassName name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<TypeSpec.Builder> builderConsumer) {
		TypeSpec.Builder builder = TypeSpec.enumBuilder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public TypeSpec poetAnnotationType(ClassName name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<TypeSpec.Builder> builderConsumer) {
		TypeSpec.Builder builder = TypeSpec.annotationBuilder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public Modifier[] poetModifiers(String modifier,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) String... modifiers) {
		String[] modifierArray = modifiers != null ? modifiers : new String[0];
		return Stream.concat(Stream.of(modifier), Stream.of(modifierArray))
				.map(it -> Modifier.valueOf(it.toUpperCase())).toArray(Modifier[]::new);
	}

	@WrapToScript
	public ClassName poetClassName(String packageName, String name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) String... names) {
		String[] nameArray = names != null ? names : new String[0];
		return ClassName.get(packageName, name, nameArray);
	}

	@WrapToScript
	public ClassName poetSimpleClassName(String className) {
		return ClassName.bestGuess(className);
	}

	@WrapToScript
	public MethodSpec poetMethod(String name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<MethodSpec.Builder> builderConsumer) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public MethodSpec poetConstructor(
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<MethodSpec.Builder> builderConsumer) {
		MethodSpec.Builder builder = MethodSpec.constructorBuilder();
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public FieldSpec poetField(TypeName typeName, String name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<FieldSpec.Builder> builderConsumer) {
		FieldSpec.Builder builder = FieldSpec.builder(typeName, name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public AnnotationSpec poetAnnotation(ClassName name,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<AnnotationSpec.Builder> builderConsumer) {
		AnnotationSpec.Builder builder = AnnotationSpec.builder(name);
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

	@WrapToScript
	public CodeBlock poetCodeBlock(
			@ScriptParameter(defaultValue = ScriptParameter.NULL) Consumer<CodeBlock.Builder> builderConsumer) {
		CodeBlock.Builder builder = CodeBlock.builder();
		Optional.ofNullable(builderConsumer).ifPresent(it -> it.accept(builder));
		return builder.build();
	}

}
