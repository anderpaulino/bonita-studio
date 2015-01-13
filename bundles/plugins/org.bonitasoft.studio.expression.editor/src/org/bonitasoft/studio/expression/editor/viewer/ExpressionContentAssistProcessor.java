/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.studio.expression.editor.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bonitasoft.studio.common.ExpressionConstants;
import org.bonitasoft.studio.expression.editor.provider.ExpressionComparator;
import org.bonitasoft.studio.expression.editor.provider.ExpressionLabelProvider;
import org.bonitasoft.studio.model.expression.Expression;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 * @author Romain Bioteau
 */
public class ExpressionContentAssistProcessor implements IContentAssistProcessor {

    private static final String DEL_PREFIX = "${";
    private static final String DEL_SUFFIX = "}";
    private final ContextInformationValidator contextInfoValidator;
    private Set<Expression> expressions;
    private final ExpressionLabelProvider labelProvider;

    public ExpressionContentAssistProcessor(IDocument document) {
        super();
        contextInfoValidator = new ContextInformationValidator(this);
        labelProvider = new ExpressionLabelProvider();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer textViewer, int documentOffset) {
        if (expressions == null) {
            return new ICompletionProposal[0];
        }
        ICompletionProposal[] proposals = null;

        proposals = buildProposals(expressions, documentOffset, textViewer);
        return proposals;

    }

    private ICompletionProposal[] buildProposals(Set<Expression> expressions, int offset, ITextViewer textViewer) {
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        List<Expression> sortedExpressions = new ArrayList<Expression>(expressions);
        Collections.sort(sortedExpressions, new ExpressionComparator());
        String content = textViewer.getDocument().get();
        boolean showAllProposals = false;
        if (offset == 0 || !Character.isLetterOrDigit(content.charAt(offset - 1))) {
            showAllProposals = true;
        }
        final StringBuilder previousString = new StringBuilder();
        if (!showAllProposals) {
            int index = offset - 1;
            while (index > 0 && Character.isLetterOrDigit(content.charAt(index - 1))) {
                index--;
            }
            for (int i = index; i < offset; i++) {
                previousString.append(content.charAt(i));
            }
        }
        for (Expression expression : sortedExpressions) {
            if (isSupportedType(expression.getType())) {
                if (!showAllProposals && expression.getName().startsWith(previousString.toString())) {
                    final String pContent = expression.getName();
                    final String replacementString = addDelimiters(pContent);
                    proposals.add(new CompletionProposal(replacementString, offset - previousString.length(), previousString.length(), replacementString
                            .length(), labelProvider.getImage(expression), labelProvider.getText(expression), null, null));
                } else if (showAllProposals) {
                    final String replacementString = addDelimiters(expression.getName());
                    proposals.add(new CompletionProposal(replacementString, offset, 0, replacementString.length(), labelProvider.getImage(expression),
                            labelProvider.getText(expression), null, null));
                }
            }
        }
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    protected String addDelimiters(String pContent) {
        return DEL_PREFIX + pContent + DEL_SUFFIX;
    }

    private boolean isSupportedType(String type) {
        return ExpressionConstants.VARIABLE_TYPE.equals(type) || ExpressionConstants.PARAMETER_TYPE.equals(type)
                || ExpressionConstants.FORM_FIELD_TYPE.equals(type);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public IContextInformation[] computeContextInformation(ITextViewer arg0, int arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return contextInfoValidator;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        return null;
    }

    public void setExpressions(Set<Expression> expressions) {
        this.expressions = expressions;
    }

}
