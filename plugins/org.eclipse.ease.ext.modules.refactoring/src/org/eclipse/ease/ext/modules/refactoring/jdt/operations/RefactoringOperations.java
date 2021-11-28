package org.eclipse.ease.ext.modules.refactoring.jdt.operations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveStaticMembersDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RefactoringOperations {
	
	public void moveStaticMembers(IMember[] members, IType destinationType, IProgressMonitor monitor) throws CoreException {
		
		RefactoringContribution refactoringContribution = RefactoringCore.getRefactoringContribution(IJavaRefactorings.MOVE_STATIC_MEMBERS);
		MoveStaticMembersDescriptor refactoringDescriptor = (MoveStaticMembersDescriptor) refactoringContribution.createDescriptor();
		
		refactoringDescriptor.setMembers(members);
		refactoringDescriptor.setDestinationType(destinationType);
		
		RefactoringStatus status = refactoringDescriptor.validateDescriptor();
		Refactoring refactoring = refactoringDescriptor.createRefactoring(status);
		status.merge(refactoring.checkInitialConditions(monitor));
		status.merge(refactoring.checkFinalConditions(monitor));
		
		Change change = refactoring.createChange(monitor);
		change.perform(monitor);
		
	}

}
