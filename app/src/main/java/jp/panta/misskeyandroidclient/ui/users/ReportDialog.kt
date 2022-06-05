package jp.panta.misskeyandroidclient.ui.users

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.DialogReportBinding
import jp.panta.misskeyandroidclient.ui.users.viewmodel.ReportViewModel
import net.pantasystem.milktea.model.user.User

class ReportDialog : AppCompatDialogFragment(){

    companion object {
        private const val EXTRA_USER_ID = "USER_ID"
        private const val EXTRA_ACCOUNT_ID = "ACCOUNT_ID"
        private const val EXTRA_TEXT = "TEXT"
        fun newInstance(userId: User.Id) : ReportDialog {
            return ReportDialog().also {
                it.arguments = Bundle().apply {
                    putString(EXTRA_USER_ID, userId.id)
                    putLong(EXTRA_ACCOUNT_ID, userId.accountId)
                }
            }
        }

        fun newInstance(userId: User.Id, text: String) : ReportDialog {
            return ReportDialog().also {
                it.arguments = Bundle().apply {
                    putString(EXTRA_USER_ID, userId.id)
                    putLong(EXTRA_ACCOUNT_ID, userId.accountId)
                    putString(EXTRA_TEXT, text)
                }
            }
        }
    }

    private var _binding: DialogReportBinding? = null
    private val binding: DialogReportBinding
        get() = _binding!!

    private val viewModel: ReportViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val view = View.inflate(dialog.context, R.layout.dialog_report, null)

        val uId = arguments?.getString(EXTRA_USER_ID)!!
        val aId = arguments?.getLong(EXTRA_ACCOUNT_ID)!!
        val comment = arguments?.getString(EXTRA_TEXT)

        viewModel.newState(User.Id(aId, uId), comment = comment)

        dialog.setContentView(view)
        _binding = DialogReportBinding.bind(view)
        binding.input.addTextChangedListener {
            viewModel.changeComment(it?.toString())
        }

        binding.sendButton.setOnClickListener {
            viewModel.submit()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.lifecycleOwner = requireActivity()
        binding.reportViewModel = viewModel

        return dialog
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        _binding?.lifecycleOwner = null
        _binding = null

    }
}