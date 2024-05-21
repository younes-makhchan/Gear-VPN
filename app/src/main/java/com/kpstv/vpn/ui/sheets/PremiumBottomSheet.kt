package com.kpstv.vpn.ui.sheets

import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.google.accompanist.insets.navigationBarsPadding
import com.kpstv.vpn.R
import com.kpstv.vpn.data.models.Plan
import com.kpstv.vpn.ui.components.AutoSizeSingleLineText
import com.kpstv.vpn.ui.components.BottomSheetState
import com.kpstv.vpn.ui.components.ThemeButton
import com.kpstv.vpn.ui.components.ThemeOutlinedButton
import com.kpstv.vpn.ui.helpers.SkuState
import com.kpstv.vpn.ui.helpers.skudummy
import com.kpstv.vpn.ui.theme.CommonPreviewTheme
import com.kpstv.vpn.ui.theme.Dimen.dp10
import com.kpstv.vpn.ui.theme.Dimen.dp100
import com.kpstv.vpn.ui.theme.Dimen.dp15
import com.kpstv.vpn.ui.theme.Dimen.dp50
import com.kpstv.vpn.ui.theme.Dimen.sp11
import com.kpstv.vpn.ui.theme.Dimen.sp12
import com.kpstv.vpn.ui.theme.Dimen.sp13
import com.kpstv.vpn.ui.theme.Dimen.sp18
import com.kpstv.vpn.ui.theme.Dimen.sp20
import com.kpstv.vpn.ui.theme.FontMediumSpanStyle
import com.kpstv.vpn.ui.theme.goldenYellow
import com.kpstv.vpn.ui.theme.goldenYellowBright
import com.kpstv.vpn.ui.theme.goldenYellowDark
import es.dmoral.toasty.Toasty
import java.util.Locale
import kotlin.math.log

@Composable
fun PremiumBottomSheet(
  premiumBottomSheet: BottomSheetState,
  isPremiumUnlocked: Boolean,
  planState: SkuState,
  onPremiumClick: (sku: String) -> Unit,
) {
  BaseBottomSheet(bottomSheetState = premiumBottomSheet) {
    val isPlanLoading = remember(planState) {
        planState is SkuState.Loading
    }
    val skudummies = arrayOf(
      skudummy("SKU123", "$19.99"),
      skudummy("SKU456", "$29.99")
    ).toList()
    val plans1 = arrayOf(
      Plan("SKU123", "Basic Plan", 1),
      Plan("SKU456", "Standard Plan", 6),
      Plan("SKU789", "Premium Plan", 12)
    ).toList()
    val s=  SkuState.Sku1(skudummies,plans1)
    val plans = remember(planState) {
      if (planState is SkuState) s.details else emptyList()
    }
    if (!isPremiumUnlocked) {
      CommonSheet(
        lottieRes = R.raw.premium_gold,
        text = stringResource(R.string.premium_text),
        buttonText = stringResource(R.string.premium_pay),
        onBuyButtonClick = onPremiumClick,
        close = { premiumBottomSheet.hide() },
        hasPremium = false,
        isLoading = false,
        isVisible = premiumBottomSheet.isVisible(),
        plans = plans
      )
    } else {
      CommonSheet(
        lottieRes = R.raw.heart_with_particles,
        text = stringResource(R.string.premium_complete_text),
        buttonText = stringResource(R.string.premium_complete_button),
        onBuyButtonClick = { premiumBottomSheet.hide() },
        close = { premiumBottomSheet.hide() },
        hasPremium = true,
      )
    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CommonSheet(
  @RawRes lottieRes: Int,
  text: String,
  buttonText: String,
  onBuyButtonClick: (sku: String) -> Unit,
  close: () -> Unit,
  hasPremium: Boolean,
  isLoading: Boolean = false,
  isVisible: Boolean = false,
  plans: List<SkuState.Sku1.Data1> = emptyList()
) {
  val context = LocalContext.current
  Log.d("plans", plans.toString(),);
  Box(
    modifier = Modifier
      .padding(horizontal = 5.dp)
      .fillMaxWidth()
      .wrapContentHeight()
      .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
      .background(MaterialTheme.colors.background)
      .padding(10.dp)
  ) {
    AnimatedContent(targetState = isLoading) { loading ->
      if (loading) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dp50),
          horizontalArrangement = Arrangement.Center
        ) {
          CircularProgressIndicator()
        }
      } else {
        val currentSelectedIndex = remember { mutableStateOf(0) }
        val plan =
          remember(currentSelectedIndex.value) { plans.getOrNull(currentSelectedIndex.value) }

        Column {
          Row {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
            val progress by animateLottieCompositionAsState(
              composition = composition,
              iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
              modifier = Modifier.size(dp100), // it needs fixed size otherwise it fills the entire screen, [cc](github.com/airbnb/lottie-android/issues/1866)
              composition = composition,
              // beware, if you are iterating (looping) lottie forever then make sure you
              // only play it when necessary (for eg: here when the bottom sheet is visible),
              // as each rendering iteration of lottie file will keep piling up the memory
              // which would eventually cause GC to run frequently making app experience laggy
              progress = if (isVisible) progress else 0f
            )
            Text(
              modifier = Modifier.align(Alignment.CenterVertically),
              text = text,
              style = MaterialTheme.typography.h4.copy(fontSize = sp18)
            )
            Spacer(modifier = Modifier.width(20.dp))
          }
          AnimatedVisibility(visible = plans.isNotEmpty()) {
            LazyColumn {
              itemsIndexed(plans) { index, item ->
                key(item.id) {
                  SubscriptionItem(
                    plan = item,
                    selected = currentSelectedIndex.value == index,
                    onSelectionChange = { currentSelectedIndex.value = index },
                    onClick = { currentSelectedIndex.value = index }
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
          ThemeButton(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            backgroundColor = goldenYellowBright,
            textColor = MaterialTheme.colors.surface,
            onClick = {
              if (hasPremium) {
                onBuyButtonClick("")
              } else if (plan?.id != null) {
                onBuyButtonClick(plan.id)
              } else {
                Toasty.error(context, context.getString(R.string.error_plan_id)).show()
              }
            },
            text = buttonText,
            enabled = !isLoading
          )
          if (!hasPremium) {
            Spacer(modifier = Modifier.height(dp15))
            ThemeOutlinedButton(
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
              text = stringResource(R.string.cancel).uppercase(),
              borderColor = MaterialTheme.colors.primary.copy(alpha = 0.8f),
              onClick = close
            )
          }
          Spacer(modifier = Modifier.navigationBarsPadding())
        }
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SubscriptionItem(
  plan: SkuState.Sku1.Data1,
  selected: Boolean,
  onSelectionChange: () -> Unit,
  onClick: () -> Unit
) {
  val secondaryColor = MaterialTheme.colors.secondary

  val selectedBorderBrush =
    remember { Brush.linearGradient(listOf(goldenYellow, goldenYellowDark)) }
  val unSelectedBorderBrush =
    remember { Brush.linearGradient(listOf(secondaryColor, secondaryColor)) }

  val borderBrush =
    remember(selected) { if (selected) selectedBorderBrush else unSelectedBorderBrush }

  val borderDp by animateDpAsState(targetValue = if (selected) 2.dp else 1.dp)

  Column(modifier = Modifier.fillMaxWidth()) {
    Spacer(modifier = Modifier.height(dp10))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(dp10))
        .border(borderDp, borderBrush, RoundedCornerShape(dp10))
        .clickable(onClick = onClick)
        .selectable(selected = selected, onClick = onSelectionChange)
        .padding(vertical = dp15, horizontal = dp15)
    ) {
      RadioButton(
        modifier = Modifier
          .padding(top = 2.dp),
        selected = selected,
        onClick = null
      )
      Spacer(modifier = Modifier.width(dp10))
      Column {
        Text(text = plan.billingName, style = MaterialTheme.typography.h4.copy(fontSize = sp18))
        AutoSizeSingleLineText(
          text = pluralStringResource(
            R.plurals.billing_description,
            plan.billingPeriodMonth,
            plan.billingPeriodMonth
          ),
          style = MaterialTheme.typography.subtitle1.copy(fontSize = sp12),
          color = MaterialTheme.colors.secondary
        )
      }

      val priceString = remember {
        buildAnnotatedString {
          withStyle(
            FontMediumSpanStyle.copy(
              fontSize = sp13,
              baselineShift = BaselineShift(0.3f),
              color = secondaryColor
            )
          ) {
            append(plan.price[0])
          }
          withStyle(FontMediumSpanStyle.copy(fontSize = sp20)) {
            append(plan.price.drop(1))
          }
          withStyle(FontMediumSpanStyle.copy(fontSize = sp11, color = secondaryColor)) {
            append("/${plan.billingPeriodMonth}m")
          }
        }
      }
      Text(
        text = priceString,
        textAlign = TextAlign.End,
        modifier = Modifier
          .fillMaxWidth()
      )
    }
    Spacer(modifier = Modifier.height(dp10))
  }
}

@Preview(name = "Premium")
@Composable
fun PreviewPremiumBottomSheet() {
  CommonPreviewTheme {
    CommonSheet(
      lottieRes = R.raw.premium_gold,
      text = stringResource(R.string.premium_text),
      buttonText = stringResource(R.string.premium_pay),
      onBuyButtonClick = { },
      close = {},
      isLoading = false,
      hasPremium = false,
      isVisible = true,
      plans = listOf(
        SkuState.Sku1.Data1(id = "sku", "Quarterly", 3, "₹129"),
        SkuState.Sku1.Data1(id = "sku", "Monthly", 1, "₹59")
      )
    )
  }
}

@Preview(name = "Premium Loading")
@Composable
fun PreviewPremiumLoadingBottomSheet() {
  CommonPreviewTheme {
    CommonSheet(
      lottieRes = R.raw.premium_gold,
      text = stringResource(R.string.premium_text),
      buttonText = stringResource(R.string.premium_pay),
      onBuyButtonClick = { },
      close = {},
      hasPremium = false,
      isLoading = true,
      isVisible = true,
    )
  }
}

@Preview(name = "Premium Purchased")
@Composable
fun PreviewPremiumPurchasedBottomSheet() {
  CommonPreviewTheme {
    CommonSheet(
      lottieRes = R.raw.heart_with_particles,
      text = stringResource(R.string.premium_complete_text),
      buttonText = stringResource(R.string.premium_complete_button),
      onBuyButtonClick = { },
      close = {},
      isVisible = true,
      hasPremium = true,
    )
  }
}