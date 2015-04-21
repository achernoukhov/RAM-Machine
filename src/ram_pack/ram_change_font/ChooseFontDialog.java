package ram_pack.ram_change_font;

import ram_pack.LanguageTranslator;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.*;
import java.awt.*;
import java.util.Locale;

public class ChooseFontDialog extends JDialog
										implements ItemListener, ChangeListener {
	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JSpinner sizeSpinner;
	private JComboBox fontComboBox;
	private JLabel lbPlain;
	private JLabel lbItalic;
	private JLabel lbBold;
	private JLabel lbBoldItalic;
	private JLabel[] allLabels = {lbPlain, lbItalic, lbBold, lbBoldItalic};

	private String chosenFontName;
	private int chosenFontSize;
	private Font oldFont;

	public ChooseFontDialog(Font initialFont) {
		super();
		oldFont = initialFont;
		chosenFontName = initialFont.getFamily();
		chosenFontSize = initialFont.getSize();

		setTitle(LanguageTranslator.getString("choose.font"));
		setContentPane(contentPane);

		setButtonActions();

		fontComboBox.addItemListener(this);
		fontComboBox.setSelectedItem(chosenFontName);

		sizeSpinner.addChangeListener(this);
		sizeSpinner.setValue(chosenFontSize);
		/*JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(sizeSpinner);
		numberEditor.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				JSpinner spinner = (JSpinner)input;
				int value = (Integer)spinner.getValue();
				return (value>0);
			}
		});
		sizeSpinner.setEditor(numberEditor);
		sizeSpinner.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				JSpinner spinner = (JSpinner)input;
				int value = (Integer)spinner.getValue();
				return (value>0);
			}
		});*/ // ÍÅ ÐÀÁÎÒÀÅÒ!!!!
	}

	private void setButtonActions() {
		getRootPane().setDefaultButton(buttonOK);
		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onOK();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		});

		// call onCancel() when cross is clicked
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onCancel();
			}
		});

		// call onCancel() on ESCAPE
		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	private void onOK() {
	// add your code here
		dispose();
	}

	private void onCancel() {
		chosenFontName = oldFont.getFontName();
		chosenFontSize = oldFont.getSize();
		dispose();
	}

	// implemented method from ItemListener
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(fontComboBox)) {
			chosenFontName = (String) e.getItem();
			fireFontChanges();
		}
	}

	// implemented method from ChangeListener
	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(sizeSpinner)) {
			JSpinner spinner = (JSpinner)e.getSource();
			int value = (Integer)spinner.getValue();
			if (value>0) {
				chosenFontSize = value;
				fireFontChanges();
			} else {
				spinner.setValue(1);
			}
		}
	}

	public static void main(String[] args) {
		ChooseFontDialog dialog = new ChooseFontDialog(new Font(LanguageTranslator.getString("dialog"),0,12));
		dialog.showDialog();
		System.exit(0);
		/*GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		String fontName = (String)fontMenu.getItemAt(i);
		f = new Font(fontName, style, size);*/
	}

	private void fireFontChanges() {

		for (JLabel lb: allLabels) {
			Font oldFont = lb.getFont();
			Font font = new Font (chosenFontName,oldFont.getStyle(),chosenFontSize);
			lb.setFont(font);
		}
	}

	public Font showDialog() {

		this.pack();
		this.setSize(400,300);
		//this.setResizable(false);
		this.setLocationByPlatform(true);
		setModal(true);
		this.setVisible(true);
		return getChosenFont();
	}

	// returns chosen font
	public Font getChosenFont() {
		Font font = new Font(chosenFontName,0,chosenFontSize);
		if (font.getSize()!=chosenFontSize)
			font = font.deriveFont((float)chosenFontSize);
		return font;
	}

	private void createUIComponents() {
        Locale l = Locale.getDefault();
		String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.getDefault());
		fontComboBox = new JComboBox(fontNames);

	}
}
