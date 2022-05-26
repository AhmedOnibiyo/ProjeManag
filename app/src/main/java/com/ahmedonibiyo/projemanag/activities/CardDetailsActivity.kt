package com.ahmedonibiyo.projemanag.activities

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.ahmedonibiyo.projemanag.R
import com.ahmedonibiyo.projemanag.firebase.FireStoreClass
import com.ahmedonibiyo.projemanag.models.Board
import com.ahmedonibiyo.projemanag.models.Card
import com.ahmedonibiyo.projemanag.models.Task
import com.ahmedonibiyo.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_card_details.*

class CardDetailsActivity : BaseActivity() {

    private lateinit var mBoardDetails: Board
    private var taskListPosition: Int = -1
    private var cardPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        getIntentData()
        setupActionBar()

        et_name_card_details.setText(mBoardDetails
            .taskList[taskListPosition]
            .cards[cardPosition].name)

        et_name_card_details.setSelection(et_name_card_details.text.toString().length)

        btn_update_card_details.setOnClickListener {
            if (et_name_card_details.text.toString().isNotEmpty())
                updateCardDetails()
            else {
                Toast.makeText(
                    this,
                    "Please enter a card name!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_card_details_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back)
            actionBar.title = mBoardDetails
                .taskList[taskListPosition]
                .cards[cardPosition].name
        }

        toolbar_card_details_activity.setNavigationOnClickListener { onBackPressed() }
    }

    private fun getIntentData() {
        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            taskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            cardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
    }

    private fun updateCardDetails() {
        val card = Card(
            et_name_card_details.text.toString(),
            mBoardDetails.taskList[taskListPosition].cards[cardPosition].createdBy,
            mBoardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo
        )

        mBoardDetails.taskList[taskListPosition].cards[cardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().updateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    fun updateTaskListSuccess() {
        hideProgressBar()

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_delete_card -> {
                alertDialogForDeleteCard(mBoardDetails.taskList[taskListPosition]
                    .cards[cardPosition].name)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteCard() {
        val cardsList: ArrayList<Card> = mBoardDetails.taskList[taskListPosition].cards

        cardsList.removeAt(cardPosition)

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        taskList[taskListPosition].cards = cardsList

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().updateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun alertDialogForDeleteCard(cardName: String) {
        val builder = AlertDialog.Builder(this)
        // set title for alert dialog
        builder.setTitle(getString(R.string.alert))
            .setMessage(
                resources.getString(
                    R.string.confirmation_message_to_delete_card,
                    cardName)
            )

            .setIcon(R.drawable.ic_alert)
            // perform positive action
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                deleteCard()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}