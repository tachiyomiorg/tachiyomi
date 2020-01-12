package eu.kanade.tachiyomi.ui.extension

import android.view.View
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.holder.SlicedHolder
import eu.kanade.tachiyomi.util.LocaleHelper
import io.github.mthli.slice.Slice
import kotlinx.android.synthetic.main.extension_card_item.*

class ExtensionHolder(view: View, override val adapter: ExtensionAdapter) :
        BaseFlexibleViewHolder(view, adapter),
        SlicedHolder {

    override val slice = Slice(card).apply {
        setColor(adapter.cardBackground)
    }

    override val viewToSlice: View
        get() = card

    init {
        ext_button.setOnClickListener {
            adapter.buttonClickListener.onButtonClick(adapterPosition)
        }
    }

    fun bind(item: ExtensionItem) {
        val extension = item.extension
        setCardEdges(item)

        // Set source name
        ext_title.text = extension.name
        version.text = extension.versionName
        lang.text = if (extension !is Extension.Untrusted) {
            LocaleHelper.getDisplayName(extension.lang, itemView.context)
        } else {
            itemView.context.getString(R.string.ext_untrusted).toUpperCase()
        }

        GlideApp.with(itemView.context).clear(image)
        if (extension is Extension.Available) {
            GlideApp.with(itemView.context)
                    .load(extension.iconUrl)
                    .into(image)
        } else {
            extension.getApplicationIcon(itemView.context)?.let { image.setImageDrawable(it) }
        }
        bindButton(item)
    }

    @Suppress("ResourceType")
    fun bindButton(item: ExtensionItem) = with(ext_button) {
        isEnabled = true
        isClickable = true
        isActivated = false

        background = VectorDrawableCompat.create(resources!!, R.drawable.button_bg_transparent, null)
        setTextColor(ContextCompat.getColorStateList(context, R.drawable.button_bg_transparent))

        val extension = item.extension

        val installStep = item.installStep
        if (installStep != null) {
            setText(when (installStep) {
                InstallStep.Pending -> R.string.ext_pending
                InstallStep.Downloading -> R.string.ext_downloading
                InstallStep.Installing -> R.string.ext_installing
                InstallStep.Installed -> R.string.ext_installed
                InstallStep.Error -> R.string.action_retry
            })
            if (installStep != InstallStep.Error) {
                isEnabled = false
                isClickable = false
            }
        } else if (extension is Extension.Installed) {
            when {
                extension.hasUpdate -> {
                    isActivated = true
                    setText(R.string.ext_update)
                }
                extension.isObsolete -> {
                    // Red outline
                    background = VectorDrawableCompat.create(resources, R.drawable.button_bg_error, null)
                    setTextColor(ContextCompat.getColorStateList(context, R.drawable.button_bg_error))

                    setText(R.string.ext_obsolete)
                }
                else -> {
                    setText(R.string.ext_details)
                }
            }
        } else if (extension is Extension.Untrusted) {
            setText(R.string.ext_trust)
        } else {
            setText(R.string.ext_install)
        }
    }

}
