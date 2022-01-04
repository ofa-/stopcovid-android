package com.lunabeestudio.stopcovid.coreui.fastitem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.databinding.ItemHorizontalRecyclerviewBinding
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class HorizontalRecyclerViewItem : AbstractBindingItem<ItemHorizontalRecyclerviewBinding>() {

    override val type: Int = R.id.item_horizontal_recyclerview

    var horizontalItems: List<GenericItem> = listOf()
    var viewPool: RecyclerView.RecycledViewPool? = null
    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemHorizontalRecyclerviewBinding {
        return ItemHorizontalRecyclerviewBinding.inflate(inflater, parent, false).apply {
            recyclerview.setRecycledViewPool(viewPool)
        }
    }

    override fun bindView(binding: ItemHorizontalRecyclerviewBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        // Avoid clipping recycler when refresh
        var fastAdapter = binding.recyclerview.adapter as? GenericFastItemAdapter
        if (fastAdapter == null) {
            fastAdapter = GenericFastItemAdapter()
            binding.recyclerview.adapter = fastAdapter
        }

        fastAdapter.setNewList(horizontalItems)
    }
}

fun horizontalRecyclerViewItem(
    block: (HorizontalRecyclerViewItem.() -> Unit)
): HorizontalRecyclerViewItem = HorizontalRecyclerViewItem().apply(block)
