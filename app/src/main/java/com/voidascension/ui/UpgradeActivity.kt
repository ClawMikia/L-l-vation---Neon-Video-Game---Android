package com.voidascension.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidascension.R
import com.voidascension.databinding.ActivityUpgradeBinding
import com.voidascension.databinding.ItemPermanentUpgradeBinding
import com.voidascension.data.PermanentUpgrade
import com.voidascension.data.SaveManager
import com.voidascension.utils.UIUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpgradeBinding
    private lateinit var adapter: UpgradeAdapter

    @Inject
    lateinit var saveManager: SaveManager

    @Inject
    lateinit var audioManager: com.voidascension.utils.AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpgradeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            audioManager.playMenuClick()
            finish()
        }
        setupRecyclerView()
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        audioManager.startBgm("menu")
    }

    private fun setupRecyclerView() {
        adapter = UpgradeAdapter(emptyList(), 0) { upgrade ->
            handlePurchase(upgrade)
        }
        binding.rvUpgrades.layoutManager = GridLayoutManager(this, 2)
        binding.rvUpgrades.adapter = adapter
    }

    private fun refreshData() {
        val currency = saveManager.getVoidShards()
        binding.tvCurrency.text = "VOID SHARDS: $currency"
        
        val upgrades = saveManager.loadPermanentUpgrades()
        adapter.updateData(upgrades, currency)
    }

    private fun handlePurchase(upgrade: PermanentUpgrade) {
        val currency = saveManager.getVoidShards()
        val isMaxed = upgrade.level >= upgrade.maxLevel
        
        if (isMaxed) {
            Toast.makeText(this, "Already at max level!", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currency >= upgrade.cost) {
            audioManager.playMenuClick() // Or play a "buy" sound if added
            if (saveManager.spendVoidShards(upgrade.cost)) {
                saveManager.purchaseUpgrade(upgrade.id)
                refreshData()
                Toast.makeText(this, "Upgrade successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Transaction failed!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Not enough shards! Need ${upgrade.cost}, have $currency", Toast.LENGTH_SHORT).show()
        }
    }
}

class UpgradeAdapter(
    private var upgrades: List<PermanentUpgrade>,
    private var currency: Int,
    private val onPurchase: (PermanentUpgrade) -> Unit
) : RecyclerView.Adapter<UpgradeAdapter.VH>() {

    fun updateData(newUpgrades: List<PermanentUpgrade>, newCurrency: Int) {
        this.upgrades = newUpgrades
        this.currency = newCurrency
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemPermanentUpgradeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemPermanentUpgradeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = upgrades.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val upg = upgrades[position]
        holder.binding.apply {
            tvUpgradeName.text = if (upg.isEpic) upg.name else "${upg.name} (LV. ${upg.level})"
            tvUpgradeDesc.text = upg.description
            
            val isMaxed = upg.level >= upg.maxLevel
            tvUpgradeCost.text = if (isMaxed) "MAX LEVEL" else "${upg.cost} SHARDS"
            
            // Epically strongest ones styling
            if (upg.isEpic) {
                root.setCardBackgroundColor(android.graphics.Color.parseColor("#1A001A"))
                tvUpgradeName.setTextColor(android.graphics.Color.parseColor("#FF00FF")) // Magenta
                tvUpgradeCost.setTextColor(android.graphics.Color.parseColor("#FFAA00")) // Gold-ish
                btnBuy.strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF00FF"))
                btnBuy.setTextColor(android.graphics.Color.parseColor("#FF00FF"))
                btnBuy.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#330033"))
            } else {
                root.setCardBackgroundColor(android.graphics.Color.parseColor("#0A0F1E"))
                tvUpgradeName.setTextColor(android.graphics.Color.parseColor("#39FF14")) // alien_green
                tvUpgradeCost.setTextColor(android.graphics.Color.parseColor("#CCFF00")) // alien_acid
                btnBuy.strokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#39FF14"))
                btnBuy.setTextColor(android.graphics.Color.parseColor("#39FF14"))
                btnBuy.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#002211"))
            }

            // Always keep enabled but visual feedback for affordance
            btnBuy.text = if (isMaxed) "MAXED" else if (upg.isEpic) "EVOLVE" else "UPGRADE"
            
            // Set alpha to show if it's maxed
            root.alpha = if (isMaxed) 0.6f else 1f
            
            // Highlight button if affordable
            if (!isMaxed && currency >= upg.cost) {
                btnBuy.alpha = 1.0f
            } else if (!isMaxed) {
                btnBuy.alpha = 0.5f
            } else {
                btnBuy.alpha = 0.3f
            }
            
            val clickListener = android.view.View.OnClickListener {
                onPurchase(upg)
            }
            
            btnBuy.setOnClickListener(clickListener)
            root.setOnClickListener(clickListener)
        }
    }
}
