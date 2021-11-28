package org.eclipse.ease.ext.modules.refactoring.jdt.operations;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitUtils {

	public static CompilationUnit toAstCompilationUnit(IFile sourceFile, IProgressMonitor monitor)
			throws CoreException {
		ICompilationUnit iCompilationUnit = JavaCore.createCompilationUnitFrom(sourceFile);
		return toAstCompilationUnit(iCompilationUnit, monitor);
	}

	public static CompilationUnit toAstCompilationUnit(IType type, IProgressMonitor monitor) throws CoreException {
		return toAstCompilationUnit(type.getCompilationUnit(), monitor);
	}

	public static CompilationUnit toAstCompilationUnit(ICompilationUnit iCompilationUnit, IProgressMonitor monitor) {

		int jslVersion = -1;
		try {
			jslVersion = AST.getJLSLatest();
		} catch (NoSuchMethodError e) {
			jslVersion = AST.JLS11;
		}

		ASTParser astParser = ASTParser.newParser(jslVersion);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setSource(iCompilationUnit);
		return (CompilationUnit) astParser.createAST(monitor);
	}

}
