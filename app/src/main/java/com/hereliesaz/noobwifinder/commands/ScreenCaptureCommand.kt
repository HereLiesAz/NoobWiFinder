package com.hereliesaz.noobwifinder.commands

import com.hereliesaz.noobwifinder.MainViewModel

class ScreenCaptureCommand(private val viewModel: MainViewModel) : Command {
    override fun execute() {
        // This will be implemented later.
        // For now, it will just call a method on the view model.
        viewModel.startScreenCapture()
    }
}
