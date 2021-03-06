package jp.co.future.ideausqlfmt;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.*;
import com.intellij.openapi.command.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.*;
import jp.co.future.ideausqlfmt.python.*;

/**
 * Provide exceptions that occurred during formatting.
 *
 * @author hoshi
 */
public class UroborosqlFormatterAction extends AnAction {
	/**
	 * PyEngine
	 */
	private PyEngine engine = new PyEngine();

	@Override
	public void actionPerformed(AnActionEvent e) {
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		Project project = e.getData(PlatformDataKeys.PROJECT);

		SelectionModel selectionModel = editor.getSelectionModel();
		String text = selectionModel.getSelectedText();
		if (text == null) {
			text = editor.getDocument().getText();
		}
		engine.put("sql", text);

		// bind python config
		PyConfigBinder binder = new PyConfigBinder();
		binder.bind(engine);

		try {
			// execute format
			engine.eval("f = uroborosqlfmt.format_sql(sql, config)");
			String fmtText = engine.get("f");

			// bind editor
			if (selectionModel.getSelectedText() == null){
				ApplicationManager.getApplication().runWriteAction(() -> {
					editor.getDocument().setText(fmtText);
				});
			} else {
				WriteCommandAction.runWriteCommandAction(project, () -> {
					editor.getDocument().replaceString(selectionModel.getSelectionStart(),
						selectionModel.getSelectionEnd(), fmtText);
				});
			}

			Notifications.Bus.notify(
				new Notification("uroborosql", "Success",
					"Format complete!", NotificationType.INFORMATION)
			);
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			Notifications.Bus.notify(
				new Notification("uroborosql", "Format failed!",
					ex.getMessage(), NotificationType.ERROR)
			);
		}

	}
}
