package com.bloom.familytasks.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.bloom.familytasks.data.models.Chore
import com.bloom.familytasks.data.models.ChoreCategory

object ChoreRepository {
    val predefinedChores = listOf(
        // Cleaning
        Chore(
            id = 1,
            name = "Vacuum Living Room",
            description = "Vacuum all carpeted areas in the living room",
            icon = Icons.Filled.CleaningServices,
            points = 1,
            category = ChoreCategory.CLEANING
        ),
        Chore(
            id = 2,
            name = "Mop Kitchen Floor",
            description = "Sweep and mop the kitchen floor thoroughly",
            icon = Icons.Filled.CleanHands,
            points = 2,
            category = ChoreCategory.KITCHEN
        ),
        Chore(
            id = 3,
            name = "Make Bed",
            description = "Make your bed neatly with pillows arranged",
            icon = Icons.Filled.Bed,
            points = 1,
            category = ChoreCategory.BEDROOM
        ),
        Chore(
            id = 4,
            name = "Load Dishwasher",
            description = "Load dirty dishes into the dishwasher properly",
            icon = Icons.Filled.Kitchen,
            points = 1,
            category = ChoreCategory.KITCHEN
        ),
        Chore(
            id = 5,
            name = "Take Out Trash",
            description = "Empty all trash bins and take bags to outside bin",
            icon = Icons.Filled.Delete,
            points = 2,
            category = ChoreCategory.CLEANING
        ),
        Chore(
            id = 6,
            name = "Water Plants",
            description = "Water all indoor and outdoor plants",
            icon = Icons.Filled.Grass,
            points = 2,
            category = ChoreCategory.OUTDOOR
        ),
        Chore(
            id = 7,
            name = "Feed Pets",
            description = "Feed and give fresh water to pets",
            icon = Icons.Filled.Pets,
            points = 1,
            category = ChoreCategory.PETS
        ),
        Chore(
            id = 8,
            name = "Organize Toys",
            description = "Put all toys back in their proper places",
            icon = Icons.Filled.Toys,
            points = 1,
            category = ChoreCategory.ORGANIZATION
        ),
        Chore(
            id = 9,
            name = "Wipe Counters",
            description = "Clean and sanitize kitchen counters",
            icon = Icons.Filled.Countertops,
            points = 2,
            category = ChoreCategory.KITCHEN
        ),
        Chore(
            id = 10,
            name = "Fold Laundry",
            description = "Fold clean laundry and put away",
            icon = Icons.Filled.LocalLaundryService,
            points = 2,
            category = ChoreCategory.BEDROOM
        )
    )

    fun getChoresByCategory(category: ChoreCategory): List<Chore> {
        return predefinedChores.filter { it.category == category }
    }

    fun getChoreById(id: Int): Chore? {
        return predefinedChores.find { it.id == id }
    }
}