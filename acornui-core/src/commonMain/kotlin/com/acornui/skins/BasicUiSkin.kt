/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.datagrid.DataGrid
import com.acornui.component.datagrid.DataGridGroupHeader
import com.acornui.component.datagrid.DataGridGroupHeaderStyle
import com.acornui.component.datagrid.DataGridStyle
import com.acornui.component.layout.DataScroller
import com.acornui.component.layout.DataScrollerStyle
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.*
import com.acornui.component.layout.algorithm.virtual.VirtualHorizontalLayoutStyle
import com.acornui.component.layout.algorithm.virtual.VirtualVerticalLayoutStyle
import com.acornui.component.layout.spacer
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.component.text.*
import com.acornui.di.ContextImpl
import com.acornui.filter.BlurQuality
import com.acornui.focus.FocusHighlighter
import com.acornui.focus.FocusableStyle
import com.acornui.focus.SimpleFocusHighlighter
import com.acornui.focus.SimpleHighlight
import com.acornui.graphic.Color
import com.acornui.input.SoftKeyboardView
import com.acornui.input.interaction.ContextMenuStyle
import com.acornui.input.interaction.ContextMenuView
import com.acornui.input.interaction.enableDownRepeat
import com.acornui.math.*
import kotlinx.coroutines.async

open class BasicUiSkin(
		protected val target: UiComponent,
		protected val theme: Theme = Theme()
) : ContextImpl(target) {

	open fun apply() {
		target.styleRules.clear()
		WindowScalingAttachment.attach(target)

		target.addStyleRule(buttonStyle { basicLabelButtonSkin(theme) }, ButtonImpl)
		target.addStyleRule(buttonStyle { basicCheckboxSkin(theme) }, CheckboxImpl)
		target.addStyleRule(buttonStyle { collapseButtonSkin(theme) }, CollapseButton)
		target.addStyleRule(buttonStyle { basicRadioButtonSkin(theme) }, RadioButtonImpl)
		target.addStyleRule(buttonStyle { basicIconButtonSkin(theme) }, IconButton)

		stageStyle()
		iconStyle()
		focusStyle()
		textFontStyle()
		textStyle()
		panelStyle()
		windowPanelStyle()
		headingGroupStyle()
		themeRectStyle()
		tabNavigatorStyle()
		dividerStyle()
		numericStepperStyle()
		scrollAreaStyle()
		scrollBarStyle()
		progressBarStyle()
		sliderStyle()
		colorPickerStyle()
		dataScrollerStyle()
		optionListStyle()
		dataGridStyle()
		rowsStyle()
		treeStyle()
		contextMenuStyle()
		calendarStyle()
		htmlComponentStyle()
		tooltipStyle()
		imageButtonStyle()
		formStyle()
		softKeyboardStyle()
		dropShadowStyle()
	}

	protected open fun stageStyle() {
		val stageStyle = StageStyle().apply {
			backgroundColor = theme.bgColor
		}
		target.addStyleRule(stageStyle)
	}

	protected open fun iconStyle() {
		val iconStyle = IconStyle().apply {
			iconColor = theme.iconColor
		}
		target.addStyleRule(iconStyle)
	}

	protected open fun focusStyle() {
		target.getAttachment<FocusHighlighter>(FocusHighlighter)?.dispose()
		val focusHighlighter = SimpleFocusHighlighter(target, theme)
		target.setAttachment(FocusHighlighter, focusHighlighter)
		val focusableStyle = FocusableStyle().apply {
			highlighter = focusHighlighter
		}
		target.addStyleRule(focusableStyle)
	}

	protected open fun textFontStyle() {
		BitmapFontRegistry.fontResolver = { request ->
			async {
				val fontFile = FontPathResolver.getPath(target, theme, request) ?: throw Exception("Font not found: $request")
				loadFontFromDir(fontFile)
			}
		}
		theme.bodyFont.addStyles()
		theme.headingFont.addStyles(withAncestor(TextStyleTags.heading))
		theme.formLabelFont.addStyles(withAncestor(formLabelStyle))
	}

	protected open fun textStyle() {
		target.addStyleRule(charStyle { selectable = theme.selectableText }, not(withAncestor(TextInput) or withAncestor(TextArea)))
		val textInputStyle = TextInputStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderColors = BorderColors(theme.stroke)
						borderThicknesses = Pad(theme.strokeThickness)
					}
				}
			}
			padding = Pad(theme.strokeThickness + 2f)
		}
		target.addStyleRule(textInputStyle, TextInput or TextArea)

		val textInputFlowStyle = TextFlowStyle()
		textInputFlowStyle.multiline = false
		target.addStyleRule(textInputFlowStyle, withAncestor(TextInput))

		val textAreaStyle = TextFlowStyle()
		textAreaStyle.multiline = true
		target.addStyleRule(textAreaStyle, withAncestor(TextArea))

		target.addStyleRule(charStyle {
			colorTint = theme.errorColor
		}, withAncestor(TextStyleTags.error))

		target.addStyleRule(charStyle {
			colorTint = theme.warningColor
		}, withAncestor(TextStyleTags.warning))

		target.addStyleRule(charStyle {
			colorTint = theme.infoColor
		}, withAncestor(TextStyleTags.info))

		target.addStyleRule(charStyle { selectable = false }, withAncestor(ButtonImpl))
		target.addStyleRule(charStyle { fontStyle = FontStyle.ITALIC }, withAncestor(TextStyleTags.emphasis))
		target.addStyleRule(charStyle { fontSize = FontSize.EXTRA_SMALL }, withAncestor(TextStyleTags.extraSmall))
		target.addStyleRule(charStyle { fontSize = FontSize.SMALL }, withAncestor(TextStyleTags.small))
		target.addStyleRule(charStyle { fontSize = FontSize.REGULAR }, withAncestor(TextStyleTags.regular))
		target.addStyleRule(charStyle { fontSize = FontSize.LARGE }, withAncestor(TextStyleTags.large))
		target.addStyleRule(charStyle { fontSize = FontSize.EXTRA_LARGE }, withAncestor(TextStyleTags.extraLarge))
	}

	protected open fun panelStyle() {
		val panelStyle = PanelStyle().apply {
			background = {
				rect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(theme.borderRadius)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(panelStyle, Panel)
	}

	protected open fun windowPanelStyle() {
		val windowPanelStyle = WindowPanelStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						val borderRadius = Corners(theme.borderRadius)
						borderRadius.topLeft.clear()
						borderRadius.topRight.clear()
						this.borderRadii = borderRadius
						val borderThickness = Pad(theme.strokeThickness)
						borderThickness.top = 0f
						this.borderThicknesses = borderThickness
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
			titleBarBackground = {
				rect {
					style.apply {
						backgroundColor = theme.controlBarBgColor
						val borderRadius = Corners(theme.borderRadius)
						borderRadius.bottomLeft.clear()
						borderRadius.bottomRight.clear()
						this.borderRadii = borderRadius
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
			closeButton = {
				iconImageButton(theme.atlasPath, "ic_clear_white_18dp") {
					style.overState = colorTransformation { tint(Color.RED) }
				}
			}
		}
		target.addStyleRule(windowPanelStyle)
	}

	protected open fun headingGroupStyle() {
		val headingGroupStyle = HeadingGroupStyle()
		headingGroupStyle.background = {
			rect {
				style.backgroundColor = theme.panelBgColor
				style.borderThicknesses = Pad(theme.strokeThickness)
				style.borderColors = BorderColors(theme.stroke)
				style.borderRadii = Corners(theme.borderRadius)
			}
		}
		headingGroupStyle.headingPadding.bottom = 0f

		headingGroupStyle.heading = {
			text {
				styleTags.add(TextStyleTags.large)
			}
		}

		target.addStyleRule(headingGroupStyle, HeadingGroup)
	}

	protected fun ThemeFontVo.addStyles(filter: StyleFilter = AlwaysFilter) {
		target.addStyleRule(charStyle {
			colorTint = color
			fontFamily = family
			fontSize = size
			fontWeight = weight
			fontStyle = style
		}, filter)

		target.addStyleRule(charStyle { fontWeight = strongWeight }, filter and withAncestor(TextStyleTags.strong))
		target.addStyleRule(charStyle { fontStyle = emphasisStyle }, filter and withAncestor(TextStyleTags.emphasis))
	}

	protected open fun themeRectStyle() {
		val themeRect = BoxStyle()
		themeRect.backgroundColor = theme.fill
		themeRect.borderColors = BorderColors(theme.stroke)
		themeRect.borderThicknesses = Pad(theme.strokeThickness)
		target.addStyleRule(themeRect, CommonStyleTags.themeRect)
	}

	protected open fun tabNavigatorStyle() {
		val tabNavStyle = TabNavigatorStyle().apply {
			tabBarPadding = Pad(0f, 0f, -theme.strokeThickness, 0f)
			contentsPadding = Pad(theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.panelBgColor
						borderColors = BorderColors(theme.stroke)
						borderThicknesses = Pad(theme.strokeThickness)
					}
				}
			}
		}
		target.addStyleRule(tabNavStyle, TabNavigator)

		target.addStyleRule(buttonStyle { basicTabSkin(theme) }, TabNavigator.DEFAULT_TAB_STYLE)
	}

	protected open fun dividerStyle() {
		val hDividerStyle = DividerStyle()
		hDividerStyle.handle = { atlas(theme.atlasPath, "HDividerHandle") }
		hDividerStyle.divideBar = { atlas(theme.atlasPath, "HDividerBar") }
		target.addStyleRule(hDividerStyle, HDivider)

		val vDividerStyle = DividerStyle()
		vDividerStyle.handle = { atlas(theme.atlasPath, "VDividerHandle") }
		vDividerStyle.divideBar = { atlas(theme.atlasPath, "VDividerBar") }
		target.addStyleRule(vDividerStyle, VDivider)

		val ruleStyle = RuleStyle()
		ruleStyle.thickness = 2f
		ruleStyle.borderColors = BorderColors(Color(1f, 1f, 1f, 0.7f))
		ruleStyle.backgroundColor = theme.stroke
		target.addStyleRule(ruleStyle)

		val vRuleStyle = RuleStyle()
		vRuleStyle.borderThicknesses = Pad().set(right = 1f)
		target.addStyleRule(vRuleStyle, Rule.VERTICAL_STYLE)

		val hRuleStyle = RuleStyle()
		hRuleStyle.borderThicknesses = Pad().set(bottom = 1f)
		target.addStyleRule(hRuleStyle, Rule.HORIZONTAL_STYLE)
	}

	protected open fun numericStepperStyle() {
		val stepperPad = Pad(4f)
		stepperPad.right = 5f

		target.addStyleRule(buttonStyle {
			val texture = basicButtonSkin(theme, Corners(topLeft = 0f, topRight = maxOf(4f, theme.borderRadius), bottomRight = 0f, bottomLeft = 0f))
			val skinPart = basicIconButtonSkin(texture) {
				layoutStyle.padding = stepperPad
			}
			val theme = theme
			skinPart.element = atlas(theme.atlasPath, "ArrowUpSm") {
				colorTint = theme.iconColor
			}
			skinPart
		}, NumericStepper.STEP_UP_STYLE)

		target.addStyleRule(buttonStyle {
			val texture = basicButtonSkin(theme, Corners(topLeft = 0f, topRight = 0f, bottomRight = maxOf(4f, theme.borderRadius), bottomLeft = 0f))
			val skinPart = basicIconButtonSkin(texture) {
				layoutStyle.padding = stepperPad
			}
			val theme = theme
			skinPart.element = atlas(theme.atlasPath, "ArrowDownSm") {
				colorTint = theme.iconColor
			}
			skinPart
		}, NumericStepper.STEP_DOWN_STYLE)
	}

	protected open fun scrollAreaStyle() {
		// Scroll area (used in GL versions)
		val scrollAreaStyle = ScrollAreaStyle()
		scrollAreaStyle.corner = {
			rect {
				style.backgroundColor = theme.strokeDisabled
			}
		}
		target.addStyleRule(scrollAreaStyle, ScrollArea)
	}

	protected open fun scrollBarStyle() {
		// Note that this does not style native scroll bars.
		val size = 10f

		val thumb: SkinPart = {
			button {
				focusEnabled = false
				style.set {
					{
						rect {
							style.backgroundColor = Color(0f, 0f, 0f, 0.6f)
							defaultWidth = size
							defaultHeight = size
						}
					}
				}
			}
		}

		val track: SkinPart = {
			rect {
				style.backgroundColor = Color(1f, 1f, 1f, 0.4f)
				defaultWidth = size
				defaultHeight = size
				enableDownRepeat()
			}
		}

		target.scrollBarStyle(VScrollBar) {
			decrementButton = { spacer(size, 0f) }
			incrementButton = { spacer(size, 0f) }
			this.thumb = thumb
			this.track = track
			inactiveAlpha = 0.2f
		}

		target.scrollBarStyle(HScrollBar) {
			decrementButton = { spacer(0f, size) }
			incrementButton = { spacer(0f, size) }
			this.thumb = thumb
			this.track = track
			inactiveAlpha = 0.2f
		}
	}

	private fun progressBarStyle() {
		val s = ProgressBarRectStyle().apply {
			borderColors = BorderColors(theme.stroke)
			borderRadii = Corners(0f)
			borderThicknesses = Pad(theme.strokeThickness)
			fillColor = theme.fill
			bgColor = Color(0f, 0f, 0f, 0.2f)
		}
		target.addStyleRule(s, ProgressBarRect)
	}

	protected open fun sliderStyle() {
		val vSliderStyle = ScrollBarStyle()
		vSliderStyle.defaultSize = 200f
		vSliderStyle.inactiveAlpha = 1f
		vSliderStyle.decrementButton = { spacer() }
		vSliderStyle.incrementButton = { spacer() }
		vSliderStyle.thumb = {
			atlas(theme.atlasPath, "SliderArrowRightLarge") {
				layoutData = basicLayoutData {}
			}
		}
		vSliderStyle.track = {
			rect {
				style.apply {
					backgroundColor = theme.fillShine
					borderThicknesses = Pad(top = 0f, right = 0f, bottom = 0f, left = 4f)
					borderColors = BorderColors(Color(0f, 0f, 0f, 0.4f))
				}
				enableDownRepeat()
				layoutData = basicLayoutData {
					width = 13f
					heightPercent = 1f
				}
			}
		}
		vSliderStyle.pageMode = false
		target.addStyleRule(vSliderStyle, VSlider)

		val hSliderStyle = ScrollBarStyle()
		hSliderStyle.defaultSize = 200f
		hSliderStyle.inactiveAlpha = 1f
		hSliderStyle.decrementButton = { spacer() }
		hSliderStyle.incrementButton = { spacer() }
		hSliderStyle.thumb = {
			atlas(theme.atlasPath, "SliderArrowDownLarge") {
				layoutData = basicLayoutData {}
			}
		}
		hSliderStyle.track = {
			rect {
				style.apply {
					backgroundColor = theme.fillShine
					borderThicknesses = Pad(top = 0f, right = 0f, bottom = 4f, left = 0f)
					borderColors = BorderColors(Color(0f, 0f, 0f, 0.4f))
				}
				enableDownRepeat()
				layoutData = basicLayoutData {
					height = 13f
					widthPercent = 1f
				}
			}
		}
		hSliderStyle.pageMode = false
		target.addStyleRule(hSliderStyle, HSlider)
	}

	protected open fun colorPickerStyle() {
		val colorPaletteStyle = ColorPaletteStyle().apply {
			background = {
				rect {
					styleTags.add(CommonStyleTags.themeRect)
					style.borderRadii = Corners(theme.borderRadius)
				}
			}
			hueSaturationIndicator = {
				atlas(theme.atlasPath, "Picker")
			}
			sliderArrow = {
				atlas(theme.atlasPath, "SliderArrowRight")
			}
		}
		target.addStyleRule(colorPaletteStyle, ColorPalette)

		val colorPickerStyle = ColorPickerStyle().apply {
			background = {
				button {
					focusEnabled = false
					style.skin = { basicButtonSkin(theme, Corners(32f), Pad(theme.strokeThickness)) }
				}
			}
			colorSwatch = {
				rect {
					style.backgroundColor = Color.WHITE
					style.borderColors = BorderColors(Color.WHITE * 0.8f)
					style.borderRadii = Corners(32f)
					style.borderThicknesses = Pad(1f)
				}
			}
		}
		target.addStyleRule(colorPickerStyle, ColorPicker)

		val colorSwatchStyle = BoxStyle()
		colorSwatchStyle.borderRadii = Corners(theme.borderRadius)
		target.addStyleRule(colorSwatchStyle, ColorPicker.COLOR_SWATCH_STYLE)
	}

	protected open fun dataScrollerStyle() {
		val dataScrollerStyle = DataScrollerStyle()
		dataScrollerStyle.padding = Pad(theme.strokeThickness)
		dataScrollerStyle.background = {
			rect {
				style.apply {
					backgroundColor = theme.panelBgColor
					borderThicknesses = Pad(theme.strokeThickness)
					borderColors = BorderColors(theme.stroke)
				}
			}
		}
		target.addStyleRule(dataScrollerStyle, DataScroller)

		val verticalLayoutStyle = VirtualVerticalLayoutStyle()
		verticalLayoutStyle.padding = Pad(top = 0f, right = 5f, bottom = 0f, left = 5f)
		target.addStyleRule(verticalLayoutStyle, withParent(DataScroller))

		val horizontalLayoutStyle = VirtualHorizontalLayoutStyle()
		horizontalLayoutStyle.padding = Pad(top = 5f, right = 0f, bottom = 5f, left = 0f)
		target.addStyleRule(horizontalLayoutStyle, withParent(DataScroller))
	}

	protected open fun optionListStyle() {
		val optionListStyle = OptionListStyle().apply {
			downArrow = {
				iconAtlas(theme.atlasPath, "ic_expand_more_white_24dp")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness + 2f, theme.strokeThickness, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderRadii = Corners(0f)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(optionListStyle, OptionList)

		val pad = Pad(top = 0f, right = theme.strokeThickness, bottom = theme.strokeThickness, left = theme.strokeThickness)
		val dataScrollerStyle = DataScrollerStyle().apply {
			padding = pad
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderThicknesses = pad
					style.borderRadii = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
					style.borderColors = BorderColors(theme.stroke)
				}
			}
			borderRadii = Corners(0f, 0f, theme.borderRadius, theme.borderRadius)
		}
		target.styleRules.add(StyleRule(dataScrollerStyle, withAncestor(OptionList)))

		val scrollRectStyle = ScrollRectStyle().apply {
			borderRadii = Corners(0f, 0f, 0f, theme.borderRadius - theme.strokeThickness)
		}
		target.styleRules.add(StyleRule(scrollRectStyle, withAncestor(OptionList)))

		val textInputStyle = TextInputStyle().apply {
			background = noSkinOptional
		}
		target.addStyleRule(textInputStyle, withAncestor(OptionList) and TextInput)
	}

	protected open fun dataGridStyle() {
		val dataGridStyle = DataGridStyle().apply {
			background = {
				rect {
					style.apply {
						backgroundColor = theme.fill
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
						borderRadii = Corners(theme.borderRadius)
					}
				}
			}
			cellPadding = Pad(theme.strokeThickness + 2f)
			resizeHandleWidth = 8f
			sortDownArrow = { atlas(theme.atlasPath, "ArrowDownMed") { colorTint = theme.iconColor } }
			sortUpArrow = { atlas(theme.atlasPath, "ArrowUpMed") { colorTint = theme.iconColor } }
			borderRadii = Corners(theme.borderRadius)
			borderThicknesses = Pad(theme.strokeThickness + 1f)
			cellFocusHighlight = {
				SimpleHighlight(target, theme.atlasPath, "FocusRect").apply { colorTint = theme.focusHighlightColor }
			}
			headerCellBackground = {
				button {
					style.skin = { basicButtonSkin(theme, Corners(0f), Pad(0f)) }
				}
			}
		}

		target.addStyleRule(dataGridStyle, DataGrid)

		target.addStyleRule(charStyle { selectable = false }, withAncestor(TextField) andThen withAncestor(DataGrid.BODY_CELL))

		val headerFlowStyle = FlowLayoutStyle()
		headerFlowStyle.horizontalAlign = FlowHAlign.CENTER
		headerFlowStyle.multiline = false
		target.addStyleRule(headerFlowStyle, withAncestor(TextField) andThen withAncestor(DataGrid.HEADER_CELL))

		target.addStyleRule(charStyle { selectable = false }, withAncestor(TextField) andThen (withAncestor(DataGridGroupHeader) or withAncestor(DataGrid.HEADER_CELL)))

		val dataGridGroupHeaderStyle = DataGridGroupHeaderStyle()
		dataGridGroupHeaderStyle.collapseButton = { collapseButton { toggleOnClick = false } }
		dataGridGroupHeaderStyle.background = {
			rect {
				style.backgroundColor = theme.controlBarBgColor
				style.borderThicknesses = Pad(0f, 0f, 1f, 0f)
				style.borderColors = BorderColors(theme.stroke)
			}
		}
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val dataGridGroupHeaderLayoutStyle = HorizontalLayoutStyle()
		dataGridGroupHeaderLayoutStyle.padding = Pad(6f)
		dataGridGroupHeaderLayoutStyle.gap = 2f
		dataGridGroupHeaderLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(dataGridGroupHeaderStyle, DataGridGroupHeader)

		val columnMoveIndicatorStyle = BoxStyle()
		columnMoveIndicatorStyle.backgroundColor = Color(0.5f, 0.5f, 0.5f, 0.5f)
		target.addStyleRule(columnMoveIndicatorStyle, DataGrid.COLUMN_MOVE_INDICATOR)

		val columnInsertionIndicatorStyle = RuleStyle()
		columnInsertionIndicatorStyle.thickness = 4f
		columnInsertionIndicatorStyle.backgroundColor = Color.DARK_GRAY
		target.addStyleRule(columnInsertionIndicatorStyle, DataGrid.COLUMN_INSERTION_INDICATOR)
	}

	protected open fun rowsStyle() {
		val rowBackgroundsStyle = RowBackgroundStyle()
		rowBackgroundsStyle.evenColor = theme.evenRowBgColor
		rowBackgroundsStyle.oddColor = theme.oddRowBgColor
		rowBackgroundsStyle.highlightedEvenColor = theme.highlightedEvenRowBgColor
		rowBackgroundsStyle.highlightedOddColor = theme.highlightedOddRowBgColor
		rowBackgroundsStyle.toggledEvenColor = theme.toggledEvenRowBgColor
		rowBackgroundsStyle.toggledOddColor = theme.toggledOddRowBgColor
		target.addStyleRule(rowBackgroundsStyle, RowBackground)
	}

	protected open fun treeStyle() {
		val itemRendererStyle = DefaultTreeItemRendererStyle()
		itemRendererStyle.openedFolderIcon = {
			atlas(theme.atlasPath, "folder-horizontal-open.png")
		}
		itemRendererStyle.closedFolderIcon = {
			atlas(theme.atlasPath, "folder-horizontal.png")
		}
		itemRendererStyle.leafIcon = {
			atlas(theme.atlasPath, "document.png")
		}
		target.addStyleRule(itemRendererStyle, DefaultTreeItemRenderer)
		val horizontalLayoutStyle = HorizontalLayoutStyle()
		horizontalLayoutStyle.verticalAlign = VAlign.MIDDLE
		target.addStyleRule(horizontalLayoutStyle, withParent(DefaultTreeItemRenderer))

		target.addStyleRule(charStyle { selectable = false }, withParent(DefaultTreeItemRenderer))
	}

	protected open fun contextMenuStyle() {
		val contextMenuStyle = ContextMenuStyle()
		contextMenuStyle.rightArrow = {
			atlas(theme.atlasPath, "ArrowRightMed")
		}
		target.addStyleRule(contextMenuStyle, ContextMenuView)
	}

	protected open fun calendarStyle() {
		val datePickerStyle = DatePickerStyle().apply {
			downArrow = {
				iconAtlas(theme.atlasPath, "ic_date_range_white_24dp")
			}
			padding = Pad(theme.strokeThickness, theme.strokeThickness + 2f, theme.strokeThickness, theme.strokeThickness)
			background = {
				rect {
					style.apply {
						backgroundColor = theme.inputFill
						borderThicknesses = Pad(theme.strokeThickness)
						borderColors = BorderColors(theme.stroke)
					}
				}
			}
		}
		target.addStyleRule(datePickerStyle, DatePicker)

		val calendarPanelStyle = PanelStyle().apply {
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(bottomLeft = Vector2(theme.borderRadius, theme.borderRadius), bottomRight = Vector2(theme.borderRadius, theme.borderRadius))
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(calendarPanelStyle, Panel and withAncestor(Calendar))

		val calendarStyle = CalendarStyle().apply {
			monthDecButton = {
				iconImageButton {
					element = atlas(theme.atlasPath, "ic_chevron_left_white_24dp")
				}
			}
			monthIncButton = {
				iconImageButton {
					element = atlas(theme.atlasPath, "ic_chevron_right_white_24dp")
				}
			}
		}
		target.addStyleRule(calendarStyle)

		val inactiveCalendarItemRendererStyle = CalendarItemRendererStyle().apply {
			disabledColor = Color(0.5f, 0.5f, 0.5f, 0.3f)
			upColor = Color(1f, 1f, 1f, 0.3f)
			overColor = Color(1f, 1f, 0.5f, 0.3f)
			downColor = Color(0.6f, 0.6f, 0.5f, 0.3f)
			toggledUpColor = Color(1f, 1f, 0f, 0.2f)
			toggledOverColor = Color(1f, 1f, 0f, 0.3f)
			toggledDownColor = Color(1f, 1f, 0f, 0.2f)
		}
		target.addStyleRule(inactiveCalendarItemRendererStyle, withAncestor(CalendarItemRendererImpl) and withAncestor(CalendarItemRendererImpl.INACTIVE))

		val calendarTextFlowStyle = TextFlowStyle().apply {
			horizontalAlign = FlowHAlign.CENTER
		}
		target.addStyleRule(calendarTextFlowStyle, withAncestor(CalendarItemRendererImpl))

		val textInputStyle = TextInputStyle()
		textInputStyle.apply {
			background = noSkinOptional
		}
		target.addStyleRule(textInputStyle, withAncestor(DatePicker) and TextInput)
	}

	protected open fun htmlComponentStyle() {
		val boxStyle = BoxStyle()
		boxStyle.backgroundColor = Color.CLEAR
		target.addStyleRule(boxStyle, HtmlComponent)
	}

	protected open fun tooltipStyle() {
		val tooltipStyle = PanelStyle().apply {
			background = {
				shadowRect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(theme.borderRadius)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(tooltipStyle, TooltipView)
	}

	protected open fun imageButtonStyle() {
		val imageButtonStyle = ImageButtonStyle().apply {
			upState = colorTransformation {
				tint(theme.iconColor)
			}
			overState = colorTransformation {
				tint(theme.iconColor)
				offset = Color(0.1f, 0.1f, 0.1f, 0.0f)
			}
			downState = colorTransformation {
				tint(theme.iconColor * 0.9f)
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}

			toggledUpState = colorTransformation {
				tint(theme.toggledIconColor)
			}
			toggledOverState = colorTransformation {
				tint(theme.toggledIconColor)
				offset = Color(0.1f, 0.1f, 0.1f, 0.0f)
			}
			toggledDownState = colorTransformation {
				tint(theme.toggledIconColor * 0.9f)
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}

			disabledState = colorTransformation {
				tint(0.2f, 0.2f, 0.2f, 0.5f)
				grayscale()
				offset = Color(-0.1f, -0.1f, -0.1f, 0.0f)
			}
		}
		target.addStyleRule(imageButtonStyle, ImageButton.ICON_IMAGE)
	}

	protected open fun formStyle() {
		val formStyle = GridLayoutStyle().apply {
			horizontalGap = 10f
		}
		target.addStyleRule(formStyle, FormContainer)
	}

	protected open fun softKeyboardStyle() {
		val panelStyle = PanelStyle().apply {
			background = {
				rect {
					style.backgroundColor = theme.panelBgColor
					style.borderColors = BorderColors(theme.stroke)
					style.borderRadii = Corners(0f)
					style.borderThicknesses = Pad(theme.strokeThickness)
				}
			}
		}
		target.addStyleRule(panelStyle, SoftKeyboardView)
	}

	protected open fun dropShadowStyle() {
		val dropShadowStyle = GlowBoxStyle().apply {
			quality = BlurQuality.NORMAL
			blurX = 3f
			blurY = 3f
			offset = Vector3(4f, 4f)
			colorTransform = ColorTransformation().apply {
				tint(Color(a = 0.3f))
			}
		}
		target.addStyleRule(dropShadowStyle, shadowRectStyleTag)
	}

}