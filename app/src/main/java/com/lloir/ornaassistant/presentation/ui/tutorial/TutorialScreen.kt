package com.lloir.ornaassistant.presentation.ui.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lloir.ornaassistant.presentation.viewmodel.TutorialViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(
    onFinish: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel()
) {
    val tutorialPages = viewModel.tutorialPages
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val isLastPage by remember { derivedStateOf { currentPage == tutorialPages.size - 1 } }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Skip button at the top right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton(
                onClick = {
                    viewModel.markTutorialAsCompleted()
                    onFinish()
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text("Skip")
            }
        }

        // Tutorial content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            TutorialPage(
                tutorialPage = tutorialPages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Page indicator
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(tutorialPages.size) { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 8.dp)
                            .background(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Next/Finish button
            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.markTutorialAsCompleted()
                        onFinish()
                    } else {
                        // Move to next page
                        viewModel.launchCoroutineScope {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(if (isLastPage) "Get Started" else "Next")
                Icon(
                    imageVector = if (isLastPage) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TutorialPage(
    tutorialPage: com.lloir.ornaassistant.presentation.viewmodel.TutorialPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tutorialPage.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = tutorialPage.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = tutorialPage.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}