package com.fbsum.plugin.viewholdergenerator

import com.fbsum.plugin.viewholdergenerator.model.Entry
import com.fbsum.plugin.viewholdergenerator.ui.ContentPanel
import com.fbsum.plugin.viewholdergenerator.util.Utils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import javax.swing.JFrame
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
            showDialog(project, entries)
        }
    }

    private fun showDialog(project: Project, entries: ArrayList<Entry>) {
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
                    entries.forEach { log.info("entry = " + it) }
                    object : WriteCommandAction.Simple<Any>(project) {
                        @Throws(Throwable::class)
                        override fun run() {
                            ViewHolderWriter(project,entries).execute()
                        }
                    }.execute()
                }

                dialog.isVisible = false
                dialog.dispose()
            }
        }
    }

}
