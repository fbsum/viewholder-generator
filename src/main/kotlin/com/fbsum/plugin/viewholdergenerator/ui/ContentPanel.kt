package com.fbsum.plugin.viewholdergenerator.ui

import com.fbsum.plugin.viewholdergenerator.model.Entry
import com.fbsum.plugin.viewholdergenerator.util.Utils
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

class ContentPanel(private val entries: ArrayList<Entry>) : JPanel() {

    private val log = Logger.getInstance(Utils::class.java)

    private val entryPanels: ArrayList<EntryPanel> = ArrayList()
    var onConfirmListener: OnConfirmListener? = null
    var onCancelListener: OnCancelListener? = null

    init {
        preferredSize = Dimension(640, 360)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

        val headPanel = HeaderPanel()
        headPanel.allCheckChangedListener = object : HeaderPanel.AllCheckChangedListener {
            override fun onAllCheckChanged(check: Boolean) {
                selectAllEntries(check)
            }
        }
        add(headPanel)

        val entryListPanel = EntryListPanel()
        val scrollPane = JBScrollPane(entryListPanel)
        add(scrollPane)

        val buttonsPanel = createButtonsPanel()
        add(buttonsPanel, BorderLayout.PAGE_END)
    }

    /**
     * 全选
     */
    private fun selectAllEntries(check: Boolean) {
        for (entry in entryPanels) {
            entry.setCheck(check)
        }
    }

    private fun createButtonsPanel(): JPanel {
        val cancelButton = JButton()
        cancelButton.action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                if (onCancelListener != null) {
                    onCancelListener!!.onCancel()
                }
            }
        }
        cancelButton.preferredSize = Dimension(120, 26)
        cancelButton.text = "Cancel"
        cancelButton.isVisible = true

        val confirmButton = JButton()
        confirmButton.action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                for (entry in entryPanels) {
                    entry.syncEntry()
                }
                if (onConfirmListener != null) {
                    onConfirmListener!!.onConfirm(entries)
                }
            }
        }
        confirmButton.preferredSize = Dimension(120, 26)
        confirmButton.text = "Confirm"
        confirmButton.isVisible = true

        val buttonsPanel = JPanel()
        buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS)
        buttonsPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        buttonsPanel.add(Box.createHorizontalGlue())
        buttonsPanel.add(cancelButton)
        buttonsPanel.add(Box.createRigidArea(Dimension(10, 0)))
        buttonsPanel.add(confirmButton)

        return buttonsPanel
    }

    /**
     * Entry List Panel
     */
    private inner class EntryListPanel : JPanel() {

        init {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            add(Box.createRigidArea(Dimension(0, 5)))

            entries.map {
                EntryPanel(it)
            }.forEach {
                add(it)
                entryPanels.add(it)
            }

            add(Box.createVerticalGlue())
            add(Box.createRigidArea(Dimension(0, 5)))
        }
    }

    /**
     * Entry Panel
     */
    private class EntryPanel(val entry: Entry) : JPanel() {

        companion object {
            private const val HEIGHT = 26
            private var ERROR_COLOR = Color(0x880000)
        }

        private val checkBox: JCheckBox
        private val typeLabel: JLabel
        private val idLabel: JLabel
        private val variableTextField: JTextField
        private val textFieldDefaultColor: Color

        init {

            checkBox = JCheckBox()
            checkBox.preferredSize = Dimension(40, HEIGHT)
            checkBox.isSelected = true
            checkBox.addChangeListener {
                checkState()
            }

            typeLabel = JLabel(entry.name)
            typeLabel.preferredSize = Dimension(100, HEIGHT)

            idLabel = JLabel(entry.id)
            idLabel.preferredSize = Dimension(100, HEIGHT)

            variableTextField = JTextField(entry.variableName, 20)
            variableTextField.preferredSize = Dimension(100, HEIGHT)
            variableTextField.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                }

                override fun focusLost(e: FocusEvent?) {
                    syncEntry()
                }
            })
            textFieldDefaultColor = variableTextField.background

            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
            maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), 54)
            add(checkBox)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(typeLabel)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(idLabel)
            add(Box.createRigidArea(Dimension(30, 0)))
            add(variableTextField)
            add(Box.createHorizontalGlue())

            checkState()
        }

        fun setCheck(check: Boolean) {
            checkBox.isSelected = check
        }

        fun syncEntry() {
            entry.used = checkBox.isSelected
            entry.variableName = variableTextField.text

            if (entry.checkValidity()) {
                variableTextField.background = textFieldDefaultColor
            } else {
                variableTextField.background = ERROR_COLOR
            }
        }

        private fun checkState() {
            if (checkBox.isSelected) {
                typeLabel.isEnabled = true
                idLabel.isEnabled = true
                variableTextField.isEnabled = true
            } else {
                typeLabel.isEnabled = false
                idLabel.isEnabled = false
                variableTextField.isEnabled = false
            }
        }

    }


    /**
     * Header Panel
     */
    private class HeaderPanel : JPanel() {

        var allCheckChangedListener: AllCheckChangedListener? = null

        init {
            val allCheckBox = JCheckBox()
            allCheckBox.preferredSize = Dimension(40, 26)
            allCheckBox.isSelected = true
            allCheckBox.addChangeListener {
                if (allCheckChangedListener != null) {
                    allCheckChangedListener!!.onAllCheckChanged(allCheckBox.isSelected)
                }
            }

            val typeLabel = JLabel("Class")
            typeLabel.preferredSize = Dimension(100, 26)
            typeLabel.font = Font(typeLabel.font.fontName, Font.BOLD, typeLabel.font.size)

            val idLabel = JLabel("ID")
            idLabel.preferredSize = Dimension(100, 26)
            idLabel.font = Font(idLabel.font.fontName, Font.BOLD, idLabel.font.size)

            val variableLabel = JLabel("Variable Name")
            variableLabel.preferredSize = Dimension(100, 26)
            variableLabel.font = Font(variableLabel.font.fontName, Font.BOLD, variableLabel.font.size)

            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
            add(Box.createRigidArea(Dimension(1, 0)))
            add(allCheckBox)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(typeLabel)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(idLabel)
            add(Box.createRigidArea(Dimension(30, 0)))
            add(variableLabel)
            add(Box.createHorizontalGlue())

            add(Box.createRigidArea(Dimension(0, 15)))
        }

        interface AllCheckChangedListener {
            fun onAllCheckChanged(check: Boolean)
        }

    }

    interface OnConfirmListener {
        fun onConfirm(entries: ArrayList<Entry>)
    }

    interface OnCancelListener {
        fun onCancel()
    }
}