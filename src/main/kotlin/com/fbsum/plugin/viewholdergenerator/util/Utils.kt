package com.fbsum.plugin.viewholdergenerator.util

import com.fbsum.plugin.viewholdergenerator.model.Entry
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.xml.XmlTag

class Utils {

    companion object {

        private val log = Logger.getInstance(Utils::class.java)

        fun isValidElement(editor: Editor, file: PsiFile): Boolean {

            val offset = editor.caretModel.offset

            val candidateA = file.findElementAt(offset)
            val candidateB = file.findElementAt(offset - 1)

            return isLayoutElement(candidateA) || isLayoutElement(candidateB)
        }

        private fun isLayoutElement(element: PsiElement?): Boolean {
            if (element == null) {
                return false
            }
            log.info("Finding layout resource for element: " + element.text)

            val layout = if (element is LeafPsiElement) {
                element.parent.parent.firstChild ?: return false
            } else {
                element.parent.firstChild ?: return false
            }
            return "R.layout" == layout.text
        }

        /**
         * Try to find layout XML file in current source on cursor's position
         */
        fun getLayoutFileFromCaret(editor: Editor, file: PsiFile): PsiFile? {

            val offset = editor.caretModel.offset

            val candidateA = file.findElementAt(offset)
            val candidateB = file.findElementAt(offset - 1)

            return when {
                isLayoutElement(candidateA) -> {
                    val name = String.format("%s.xml", candidateA!!.text)
                    resolveLayoutResourceFile(candidateA, candidateA.project, name)
                }
                isLayoutElement(candidateB) -> {
                    val name = String.format("%s.xml", candidateB!!.text)
                    resolveLayoutResourceFile(candidateB, candidateB.project, name)
                }
                else -> null
            }
        }

        /**
         * Try to find layout XML file in selected element
         */
        private fun resolveLayoutResourceFile(element: PsiElement, project: Project, name: String): PsiFile? {
            // restricting the search to the current module - searching the whole project could return wrong layouts
            val module = ModuleUtil.findModuleForPsiElement(element)
            var files: Array<PsiFile>? = null
            if (module != null) {
                // first omit libraries, it might cause issues like (#103)
                var moduleScope = module.moduleWithDependenciesScope
                files = FilenameIndex.getFilesByName(project, name, moduleScope)
                if (files == null || files.isEmpty()) {
                    // now let's do a fallback including the libraries
                    moduleScope = module.getModuleWithDependenciesAndLibrariesScope(false)
                    files = FilenameIndex.getFilesByName(project, name, moduleScope)
                }
            }
            if (files == null || files.isEmpty()) {
                // fallback to search through the whole project
                // useful when the project is not properly configured - when the resource directory is not configured
                files = FilenameIndex.getFilesByName(project, name, EverythingGlobalScope(project))
                if (files.isEmpty()) {
                    return null //no matching files
                }
            }

            // TODO - we have a problem here - we still can have multiple layouts (some coming from a dependency)
            // we need to resolve R class properly and find the proper layout for the R class
            for (file in files) {
                log.info("Resolved layout resource file for name [" + name + "]: " + file.virtualFile)
            }
            return files[0]
        }

        /**
         * Obtain all IDs from layout
         */
        fun getEntriesFromLayout(file: PsiFile): ArrayList<Entry> {
            return getEntriesFromLayout(file, ArrayList())
        }

        /**
         * Obtain all IDs from layout file
         */
        private fun getEntriesFromLayout(layoutFile: PsiFile, entries: ArrayList<Entry>): ArrayList<Entry> {
            layoutFile.accept(object : XmlRecursiveElementVisitor() {

                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    if (element !is XmlTag) {
                        return
                    }

                    if (element.name.equals("include", ignoreCase = true)) {
                        val layout = element.getAttribute("layout", null)
                        if (layout != null) {
                            val project = layoutFile.project
                            var newLayoutFileName = getLayoutName(layout.value)
                            if (newLayoutFileName != null) {
                                newLayoutFileName = String.format("%s.xml", newLayoutFileName)
                                val include = resolveLayoutResourceFile(layoutFile, project, newLayoutFileName)
                                if (include != null) {
                                    getEntriesFromLayout(include, entries)
                                    return
                                }
                            }
                        }
                    }

                    // get element ID
                    val id = element.getAttribute("android:id", null) ?: return  // missing android:id attribute
                    val value = id.value ?: return  // empty value

                    // check if there is defined custom class
                    var name = element.name
                    val clazz = element.getAttribute("class", null)
                    if (clazz != null && clazz.value != null) {
                        name = clazz.value!!
                    }

                    try {
                        entries.add(Entry(name, value))
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        log.info("Add element error...")
                    }
                }
            })
            return entries
        }

        /**
         * Get layout name from XML identifier (@layout/....)
         */
        private fun getLayoutName(layout: String?): String? {
            if (layout == null || !layout.startsWith("@") || !layout.contains("/")) {
                return null // it's not layout identifier
            }

            val parts = layout.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (parts.size != 2) {
                null // not enough parts
            } else {
                parts[1]
            }
        }
    }
}
