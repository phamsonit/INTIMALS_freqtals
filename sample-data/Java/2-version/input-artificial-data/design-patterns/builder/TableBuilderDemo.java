package builder;

import java.awt.Component;
import javax.swing.JFrame;

public class TableBuilderDemo {

    public static void main(String[] args) {
        (new TableBuilderDemo()).demo(args);
    }
	
	/**
	     * Client code perspective.
	     */
	    public void demo(String[] args) {
	        // Name of the GUI table class can be passed to the app parameters.
	        String class_name = args.length > 0 ? args[0] : "JTable_Builder";

	        Builder target = null;
	        try {
	            target = (Builder) Class.forName("builder."+class_name)
	                    .getDeclaredConstructor().newInstance();
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }

	        //String file_name = getClass().getResource("BuilderDemo.dat").getFile();
			String file_name = "BuilderDemo.dat";
	        TableDirector director = new TableDirector(target);
	        director.construct(file_name);
	        Component comp = target.get_result();

	        JFrame frame = new JFrame("BuilderDemo - " + class_name);
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.getContentPane().add(comp);
	        frame.pack();
	        frame.setVisible(true);
	    }
}