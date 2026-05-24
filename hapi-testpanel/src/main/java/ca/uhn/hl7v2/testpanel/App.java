/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License.
 *
 * The Original Code is ""  Description:
 * ""
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2001.  All Rights Reserved.
 *
 * Contributor(s): ______________________________________.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * GNU General Public License (the  "GPL"), in which case the provisions of the GPL are
 * applicable instead of those above.  If you wish to allow use of your version of this
 * file only under the terms of the GPL and not to allow others to use your version
 * of this file under the MPL, indicate your decision by deleting  the provisions above
 * and replace  them with the notice and other provisions required by the GPL License.
 * If you do not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the GPL.
 */
package ca.uhn.hl7v2.testpanel;


import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import javax.swing.UIManager;

import org.apache.log4j.xml.DOMConfigurator;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.ui.ImageFactory;
import ca.uhn.hl7v2.util.MessageIDGenerator;

public class App {
	
//	private static final Logger ourLog = LoggerFactory.getLogger(App.class);
	private static Controller myController;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "HAPI TestPanel");
		System.setProperty(MessageIDGenerator.NEVER_FAIL_PROPERTY, Boolean.TRUE.toString());

		// HiDPI/scaling support for Linux (Wayland, X11 with fractional scaling)
		String osNameForDpi = System.getProperty("os.name", "").toLowerCase();
		if (!osNameForDpi.contains("mac") && !osNameForDpi.contains("windows")) {
			if (System.getProperty("sun.java2d.uiScale.enabled") == null) {
				System.setProperty("sun.java2d.uiScale.enabled", "true");
			}
			if (System.getProperty("swing.aatext") == null) {
				System.setProperty("swing.aatext", "true");
			}
			if (System.getProperty("awt.useSystemAAFontSettings") == null) {
				System.setProperty("awt.useSystemAAFontSettings", "on");
			}
		}

		try {
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("mac")) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				boolean dark = resolveUseDark(Prefs.getInstance().getTheme());
				if (dark) {
					FlatDarculaLaf.install();
				} else {
					FlatIntelliJLaf.install();
				}
			}
		} catch (Exception e) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

		System.setProperty("tespanel.log.dir", Prefs.getTestpanelHomeDirectory().getAbsolutePath());
		DOMConfigurator.configure(App.class.getClassLoader().getResource("log4j_testpanel.xml"));
		
		myController = new Controller();

		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			new OSXInitializer().run(myController);
		}
		
		EventQueue.invokeLater(() -> {
            try {
                myController.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

	}

	public static boolean resolveUseDark(String themePref) {
		if ("dark".equals(themePref)) return true;
		if ("light".equals(themePref)) return false;
		return isSystemDarkMode();
	}

	public static void applyTheme(String themePref) {
		boolean dark = resolveUseDark(themePref);
		try {
			if (dark) {
				FlatDarculaLaf.setup();
			} else {
				FlatIntelliJLaf.setup();
			}
			ImageFactory.clearCache();
			FlatLaf.updateUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isCurrentlyDark() {
		return UIManager.getLookAndFeel() instanceof FlatDarkLaf;
	}

	public static boolean isSystemDarkMode() {
		// GNOME / freedesktop color-scheme
		try {
			Process p = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
					.redirectErrorStream(true).start();
			String out = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
			if (out != null && out.contains("dark")) return true;
		} catch (Exception ignored) {}

		// KDE
		try {
			Process p = new ProcessBuilder("kreadconfig5", "--group", "General", "--key", "ColorScheme")
					.redirectErrorStream(true).start();
			String out = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
			if (out != null && out.toLowerCase().contains("dark")) return true;
		} catch (Exception ignored) {}

		// GTK_THEME env var
		String gtkTheme = System.getenv("GTK_THEME");
		if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) return true;

		return false;
	}

}
