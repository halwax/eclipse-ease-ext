package org.eclipse.ease.ext.modules.refactoring.operations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class SourceOperations {
	
	public void addGetter(final CompilationUnit unit, final IType type, final IField field, IProgressMonitor monitor) throws CoreException {
		IField[] getters  = new IField[] { field };
		IField[] setters = new IField[] {};
		runAddGetterSetterOperation(type, unit, monitor, getters, setters);
	}
	
	public void addSetter(final CompilationUnit unit, final IType type, final IField field, IProgressMonitor monitor) throws CoreException {
		IField[] getters  = new IField[] {};
		IField[] setters = new IField[] { field };
		runAddGetterSetterOperation(type, unit, monitor, getters, setters);
	}

	private void runAddGetterSetterOperation(final IType type, final CompilationUnit unit, IProgressMonitor monitor, IField[] getters, IField[] setters) throws CoreException {
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject());
		IRequestQuery skipExistingQuery = new IRequestQuery() {
			
			@Override
			public int doQuery(IMember member) {
				return IRequestQuery.YES_ALL;
			}
		};
		IField[] accessors = new IField[] {};
		new AddGetterSetterOperation(type, getters, setters, accessors , unit, skipExistingQuery, null, settings, true, true).run(monitor);
	}

}
