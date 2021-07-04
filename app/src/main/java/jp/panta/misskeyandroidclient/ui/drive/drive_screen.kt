package jp.panta.misskeyandroidclient.ui.drive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.asLiveData
import jp.panta.misskeyandroidclient.viewmodel.drive.DriveViewModel
import jp.panta.misskeyandroidclient.viewmodel.drive.directory.DirectoryViewModel
import jp.panta.misskeyandroidclient.viewmodel.drive.file.FileViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.model.drive.FileProperty
import jp.panta.misskeyandroidclient.ui.DirectoryListScreen


@ExperimentalCoroutinesApi
@Composable
fun DriveScreen(
    driveViewModel: DriveViewModel,
    fileViewModel: FileViewModel,
    directoryViewModel: DirectoryViewModel,
    onNavigateUp: ()->Unit,
    onFixSelected: ()->Unit
) {

    val isSelectMode: Boolean by  driveViewModel.isSelectMode.asLiveData().observeAsState(initial = false)
    val selectableMaxCount = driveViewModel.selectable?.selectableMaxSize
    val selectedFileIds: Set<FileProperty.Id>? by fileViewModel.selectedFileIds.asLiveData().observeAsState(initial = emptySet())

    var currentTabIndex: Int by remember {
        mutableStateOf(0)
    }
    Scaffold(
        topBar = {

            Column {
                val tabTitles = listOf(
                    stringResource(id = R.string.file),
                    stringResource(id = R.string.folder)
                )
                TopAppBar (
                    title = {
                        if(isSelectMode) {
                            Text("${stringResource(R.string.selected)} ${selectedFileIds?.size?: 0}/${selectableMaxCount}")
                        }else{
                            Text(stringResource(id = R.string.drive))
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onNavigateUp.invoke()
                            },
                        ) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if(isSelectMode) {
                            IconButton(onClick = onFixSelected) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Fix")
                            }
                        }
                    },
                    elevation = 0.dp

                )
                TabRow(selectedTabIndex = currentTabIndex) {
                    tabTitles.forEachIndexed { index, s ->
                        Tab(
                            text = {  Text(text = s) },
                            selected = index == currentTabIndex,
                            onClick = {
                                currentTabIndex = index
                            }

                        )
                    }

                }
            }
        },

    ) {

        if(currentTabIndex == 0) {
            FilePropertyListScreen(fileViewModel = fileViewModel, driveViewModel = driveViewModel)
        }else{
            DirectoryListScreen(viewModel = directoryViewModel, driveViewModel = driveViewModel)
        }
    }
}



