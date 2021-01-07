package eu.kanade.tachiyomi.ui.browse.extension.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ExtensionDetailHeaderBinding
import eu.kanade.tachiyomi.ui.browse.extension.getApplicationIcon
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks

class ExtensionDetailsHeaderAdapter(private val presenter: ExtensionDetailsPresenter) :
    RecyclerView.Adapter<ExtensionDetailsHeaderAdapter.HeaderViewHolder>() {

    private lateinit var binding: ExtensionDetailHeaderBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = ExtensionDetailHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val extension = presenter.extension ?: return
            val context = view.context

            extension.getApplicationIcon(context)?.let { binding.extensionIcon.setImageDrawable(it) }
            binding.extensionTitle.text = extension.name
            binding.extensionVersion.text = context.getString(R.string.ext_version_info, extension.versionName)
            binding.extensionLang.text = context.getString(R.string.ext_language_info, LocaleHelper.getSourceDisplayName(extension.lang, context))
            binding.extensionNsfw.isVisible = extension.isNsfw
            binding.extensionPkg.text = extension.pkgName

            binding.extensionUninstallButton.clicks()
                .onEach { presenter.uninstallExtension() }
                .launchIn(presenter.presenterScope)

            if (extension.isObsolete) {
                binding.extensionWarningBanner.isVisible = true
                binding.extensionWarningBanner.setText(R.string.obsolete_extension_message)
            }

            if (extension.isUnofficial) {
                binding.extensionWarningBanner.isVisible = true
                binding.extensionWarningBanner.setText(R.string.unofficial_extension_message)
            }
        }
    }
}
