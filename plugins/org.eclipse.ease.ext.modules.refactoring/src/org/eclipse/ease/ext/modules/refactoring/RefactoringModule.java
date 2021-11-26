package org.eclipse.ease.ext.modules.refactoring;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ext.modules.jdt.JDTModule;
import org.eclipse.ease.ext.modules.refactoring.operations.RefactoringOperations;
import org.eclipse.ease.ext.modules.refactoring.operations.SourceOperations;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class RefactoringModule extends AbstractScriptModule {

	private final SourceOperations sourceOperations = new SourceOperations();

	private final RefactoringOperations refactoringOperations = new RefactoringOperations();

	private final JDTModule jdtModule = new JDTModule();

	@Override
	public void initialize(final IScriptEngine engine, final IEnvironment environment) {
		super.initialize(engine, environment);
		jdtModule.initialize(engine, environment);
	}

	@WrapToScript
	public CompilationUnit toAstCompilationUnit(IFile sourceFile, IProgressMonitor monitor) throws CoreException {
		ICompilationUnit iCompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
		return toAstCompilationUnit(iCompilationUnit, monitor);
	}

	private CompilationUnit toAstCompilationUnit(ICompilationUnit iCompilationUnit, IProgressMonitor monitor) {
		ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setSource(iCompilationUnit);
		return (CompilationUnit) astParser.createAST(monitor);
	}

	@WrapToScript
	public void addGetter(CompilationUnit unit, IType type, IField field, IProgressMonitor monitor)
			throws CoreException {
		sourceOperations.addGetter(unit, type, field, monitor);
	}

	@WrapToScript
	public void addSetter(CompilationUnit unit, IType type, IField field, IProgressMonitor monitor)
			throws CoreException {
		sourceOperations.addSetter(unit, type, field, monitor);
	}

	@WrapToScript
	public PackageDeclaration setPackageDeclaration(CompilationUnit compilationUnit, String packageName) {
		AST ast = compilationUnit.getAST();
		PackageDeclaration packageDeclaration = ast.newPackageDeclaration();
		packageDeclaration.setName(ast.newSimpleName(packageName));
		compilationUnit.setPackage(packageDeclaration);
		return packageDeclaration;
	}

	@WrapToScript
	public TypeDeclaration addTypeDeclaration(CompilationUnit compilationUnit, String typeName) {
		AST ast = compilationUnit.getAST();
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(typeName));
		compilationUnit.types().add(typeDeclaration);

		return typeDeclaration;
	}

	@WrapToScript
	public void addModifier(BodyDeclaration bodyDeclaration, ModifierKeyword modifierKeyword) {
		AST ast = bodyDeclaration.getAST();
		bodyDeclaration.modifiers().add(ast.newModifier(modifierKeyword));
	}

	@WrapToScript
	public void applySaveActions(IFile file, IProgressMonitor monitor) throws CoreException {

		FileEditorInput input = new FileEditorInput(file);
		IDocumentProvider documentProvider = JavaUI.getDocumentProvider();
		documentProvider.connect(input);
		try {
			documentProvider.saveDocument(monitor, input, documentProvider.getDocument(input), true);
		} finally {
			documentProvider.disconnect(input);
		}
	}

	@WrapToScript
	public void recordAndSaveModifications(IFile file, Consumer<CompilationUnit> compilationUnitConsumer,
			IProgressMonitor monitor) throws CoreException, IOException, MalformedTreeException, BadLocationException {

		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);

		unit.becomeWorkingCopy(monitor);
		try {
			IBuffer buffer = unit.getBuffer();

			CompilationUnit astCompilationUnit = toAstCompilationUnit(unit, monitor);
			IDocument document = new Document(buffer.getContents());
			Map<String, String> options = getJavaOptionsForFile(file);

			astCompilationUnit.recordModifications();

			// applies source code modifications
			compilationUnitConsumer.accept(astCompilationUnit);

			TextEdit textEdit = astCompilationUnit.rewrite(document, options);
			textEdit.apply(document);

			String newContents = document.get(); // apply changes
			buffer.setContents(newContents);

			unit.reconcile(ICompilationUnit.NO_AST, false /* don't force problem detection */, null, monitor);

			unit.commitWorkingCopy(false, monitor);
			unit.save(monitor, false);

		} finally {
			unit.discardWorkingCopy();
		}
	}

	@WrapToScript
	public void moveStaticMembers(IMember[] members, IType destinationType, IProgressMonitor monitor)
			throws CoreException {
		refactoringOperations.moveStaticMembers(members, destinationType, monitor);
	}

	private Map<String, String> getJavaOptionsForFile(IFile file) throws CoreException {
		Map<String, String> options = JavaCore.getOptions();
		Optional<IJavaProject> javaProjectOpt = jdtModule.tryGetJavaProject(file.getProject());
		if (javaProjectOpt.isPresent()) {
			IJavaProject javaProject = javaProjectOpt.get();
			options = javaProject.getOptions(true);
		}
		return options;
	}

}
