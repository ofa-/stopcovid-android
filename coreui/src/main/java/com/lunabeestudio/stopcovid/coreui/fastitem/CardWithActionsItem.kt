package com.lunabeestudio.stopcovid.coreui.fastitem

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.LayoutDirection
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.databinding.ItemActionBinding
import com.lunabeestudio.stopcovid.coreui.databinding.ItemCardWithActionsBinding
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.coreui.extension.setOnClickListenerOrHideRipple
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class CardWithActionsItem(private val cardTheme: CardTheme) : AbstractBindingItem<ItemCardWithActionsBinding>() {

    override val type: Int = cardTheme.name.hashCode()

    var cardTitle: String? = null

    @DrawableRes
    var cardTitleIcon: Int? = null

    @ColorRes
    var cardTitleColorRes: Int? = null

    // Gradient background, override theme
    var gradientBackground: GradientDrawable? = null

    var mainTitle: String? = null
    var mainHeader: String? = null
    var mainBody: String? = null
    var mainMaxLines: Int? = null
    var mainLayoutDirection: Int = LayoutDirection.INHERIT
    var mainGravity: Int = Gravity.NO_GRAVITY

    @DrawableRes
    var mainImage: Int? = null

    var onCardClick: (() -> Unit)? = null
    var onDismissClick: (() -> Unit)? = null
    var contentDescription: String? = null

    var actions: List<Action>? = emptyList()

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemCardWithActionsBinding {
        val context = inflater.context
        val themedInflater = LayoutInflater.from(ContextThemeWrapper(context, cardTheme.themeId))
        return ItemCardWithActionsBinding.inflate(themedInflater, parent, false)
    }

    override fun bindView(binding: ItemCardWithActionsBinding, payloads: List<Any>) {
        binding.cardTitleTextView.setTextOrHide(cardTitle) {
            cardTitleColorRes?.let {
                val color = ContextCompat.getColor(context, it)
                setTextColor(color)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
            }
            setCompoundDrawablesWithIntrinsicBounds(cardTitleIcon ?: 0, 0, 0, 0)
        }

        var mainLayoutVisible = false
        binding.mainHeaderTextView.setTextOrHide(mainHeader) { mainLayoutVisible = true }
        binding.mainTitleTextView.setTextOrHide(mainTitle) { mainLayoutVisible = true }
        binding.mainBodyTextView.setTextOrHide(mainBody) {
            mainLayoutVisible = true
            this@CardWithActionsItem.mainMaxLines?.let { maxLines = it }
        }
        binding.mainImageView.setImageResourceOrHide(mainImage)
        binding.mainHeaderTextView.gravity = mainGravity
        binding.mainTitleTextView.gravity = mainGravity
        binding.mainBodyTextView.gravity = mainGravity

        if (mainLayoutVisible) {
            binding.mainLayout.visibility = View.VISIBLE
            binding.mainLayout.layoutDirection = mainLayoutDirection
            binding.contentLayout.setOnClickListenerOrHideRipple(
                onCardClick?.let {
                    View.OnClickListener {
                        it()
                    }
                }
            )
        } else {
            binding.mainLayout.visibility = View.GONE
        }

        binding.contentLayout.isVisible = (mainLayoutVisible || binding.cardTitleTextView.isVisible)

        if (actions.isNullOrEmpty()) {
            binding.actionsLinearLayout.visibility = View.GONE
        } else {
            val count = actions?.size ?: 0
            val viewCount = binding.actionsLinearLayout.childCount

            if (count < viewCount) {
                for (i in count until viewCount) {
                    binding.actionsLinearLayout.removeViewAt(0)
                }
            } else if (count > viewCount) {
                for (i in viewCount until count) {
                    ItemActionBinding.inflate(
                        LayoutInflater.from(binding.root.context),
                        binding.actionsLinearLayout,
                        true
                    )
                }
            }

            actions?.forEachIndexed { index, (icon, label, showBadge, loading, onClickListener) ->
                val actionBinding = ItemActionBinding.bind(binding.actionsLinearLayout.getChildAt(index))
                actionBinding.actionDivider.isVisible = (index == 0 && mainLayoutVisible) || (index > 0)
                actionBinding.textView.text = label.safeEmojiSpanify()
                actionBinding.leftIconImageView.setImageResourceOrHide(icon)
                actionBinding.badgeView.isVisible = showBadge
                actionBinding.actionRootLayout.setOnClickListenerOrHideRipple(onClickListener)
                actionBinding.arrowImageView.isVisible = onClickListener != null
                actionBinding.progressBar.isVisible = loading == true
            }
            if (!binding.actionsLinearLayout.isVisible) {
                binding.actionsLinearLayout.visibility = View.VISIBLE
            }
        }

        gradientBackground?.let {
            binding.rootLayout.background = it
        }

        binding.rootLayout.background = gradientBackground ?: cardTheme.backgroundDrawableRes?.let {
            ContextCompat.getDrawable(binding.rootLayout.context, it)
        }

        binding.dismissImageView.setOnClickListener(
            onDismissClick?.let { onDismiss ->
                View.OnClickListener { onDismiss() }
            }
        )
        binding.dismissImageView.isVisible = onDismissClick != null

        if (onDismissClick != null && cardTitle == null) {
            setDismissButtonMargin(binding)
        }
    }

    private fun setDismissButtonMargin(binding: ItemCardWithActionsBinding) {
        (
            binding.mainHeaderTextView.takeIf { mainHeader != null }
                ?: binding.mainTitleTextView.takeIf { mainTitle != null }
                ?: binding.mainBodyTextView.takeIf { mainBody != null }
            )?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginEnd = R.dimen.min_touch_target_size.toDimensSize(binding.rootLayout.context).toInt()
            topMargin = R.dimen.spacing_medium.toDimensSize(binding.rootLayout.context).toInt()
        }
    }

    override fun unbindView(binding: ItemCardWithActionsBinding) {
        super.unbindView(binding)
        binding.mainBodyTextView.maxLines = Int.MAX_VALUE
        binding.mainBodyTextView.visibility = View.VISIBLE
        binding.cardTitleTextView.apply {
            val color = R.attr.colorAccent.fetchSystemColor(context)
            setTextColor(color)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
        }

        listOf(binding.mainHeaderTextView, binding.mainTitleTextView).forEach {
            it.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = 0
                topMargin = 0
            }
        }
        binding.mainBodyTextView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginEnd = 0
            topMargin = R.dimen.spacing_small.toDimensSize(binding.rootLayout.context).toInt()
        }
    }
}

fun cardWithActionItem(
    cardTheme: CardTheme = CardTheme.Default,
    block: (CardWithActionsItem.() -> Unit),
): CardWithActionsItem = CardWithActionsItem(cardTheme)
    .apply(block)