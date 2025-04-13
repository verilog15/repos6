package ch.rmy.android.http_shortcuts.activities.importexport

import ch.rmy.android.framework.viewmodel.ViewModelEvent

abstract class ImportExportEvent : ViewModelEvent() {
    object OpenFilePickerForImport : ImportExportEvent()
}
