package com.fbsum.plugin.viewholdergenerator

import com.fbsum.plugin.viewholdergenerator.model.Entry
import com.fbsum.plugin.viewholdergenerator.ui.ContentPanel
import com.fbsum.plugin.viewholdergenerator.util.Utils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.WindowConstants

class ViewHolderGenerateAction : AnAction() {

    companion object {
        private val log = Logger.getInstance(ViewHolderGenerateAction::class.java)

        private val VALID_FILE_EXT = arrayOf("java", "kt")
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        event.presentation.isVisible = false
        val project = event.project ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return
        val virtualFile = event.getData(LangDataKeys.VIRTUAL_FILE) ?: return
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return

        val isValidFile = isValidFile(virtualFile)
        log.info("isValidFile = " + isValidFile)
        val isValidElement = isValidElement(editor, psiFile)
        log.info("isValidElement = " + isValidElement)

        event.presentation.isVisible = isValidFile && isValidElement
    }

    /**
     * 判断当前文件是否为 VALID_FILE_EXT 后缀
     */
    private fun isValidFile(file: VirtualFile): Boolean {
        return VALID_FILE_EXT.contains(file.extension)
    }

    /**
     * 判断当前光标所在位置能找到 Android 布局文件
     */
    private fun isValidElement(editor: Editor, psiFile: PsiFile): Boolean {
        return Utils.isValidElement(editor, psiFile)
    }


    /**
     * 执行插件
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT) ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return
        val layoutFile = Utils.getLayoutFileFromCaret(editor, psiFile) ?: return
        val entries = Utils.getEntriesFromLayout(layoutFile)
        log.info("entries = " + entries)
        if (entries.isNotEmpty()) {
            showDialog(entries)
        }
    }

    private fun showDialog(entries: ArrayList<Entry>) {
        val contentPanel = ContentPanel(entries)

        val dialog = JFrame()
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.contentPane.add(contentPanel)
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true

        contentPanel.onCancelListener = object : ContentPanel.OnCancelListener {
            override fun onCancel() {
                dialog.isVisible = false
                dialog.dispose()
            }
        }
        contentPanel.onConfirmListener = object : ContentPanel.OnConfirmListener {
            override fun onConfirm(entries: ArrayList<Entry>) {
                if (entries != null && entries.isNotEmpty()) {
                    genViewHolder(entries)
                }
                dialog.isVisible = false
                dialog.dispose()
            }
        }
    }

    private fun genViewHolder(entries: ArrayList<Entry>) {
        val sb = StringBuilder()
        sb.append("private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {")
        sb.appendln()
        entries.forEach {
            sb.append("    val ${it.variableName} = itemView.findViewById<${it.name}>(${it.fullID})")
            sb.appendln()
        }
        sb.append("}")

        showResultDialog(sb.toString())
    }

    private fun showResultDialog(content: String) {
        val textArea = JTextArea()
        textArea.append(content)

        val panel = JBScrollPane(textArea)
        panel.preferredSize = Dimension(640, 360)
        panel.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

        // frame
        val frame = JFrame()
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.contentPane.add(panel)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

}
