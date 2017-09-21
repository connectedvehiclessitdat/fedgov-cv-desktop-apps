package gov.usdot.desktop.apps.gui.controls;

import java.util.regex.Pattern;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class NumericTextField extends JTextField {

	private static final long serialVersionUID = 1L;

	@Override
    protected Document createDefaultModel() {
        return new NumericDocument();
    }

    private static class NumericDocument extends PlainDocument {

		private static final long serialVersionUID = 1L;
		
		private final static Pattern numericPattern = Pattern.compile("[-+]?\\d*[.]?\\d*");

        @Override
        public void insertString(int offset, String text, AttributeSet a) throws BadLocationException {
            if (text != null) {
                int currentBufferLength = getLength();
                String currentBufferContent = getText(0, currentBufferLength);
                String newText;

                if (currentBufferLength == 0) {
                    newText = text;
                } else {
                    newText = new StringBuilder(currentBufferContent)
                        .insert(offset, text)
                        .toString();
                }
                if (numericPattern.matcher(newText).matches())
                    super.insertString(offset, text, a);
            }
        }
    }
}