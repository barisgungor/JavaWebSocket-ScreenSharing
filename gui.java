



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class gui extends JFrame {


	private boolean booleanToChange = false;

    private JButton exampleButton;

    public gui() {
        exampleButton = new JButton();
        exampleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //Access a member in anonymous class
                gui.this.booleanToChange = true;
            }
        });
    }
	
	

}
