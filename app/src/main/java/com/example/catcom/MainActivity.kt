package com.example.catcom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.catcom.ui.adoption.AdoptionFormScreen
import com.example.catcom.ui.adoption.AdoptionListScreen
import com.example.catcom.ui.adoption.AdoptionViewModel
import com.example.catcom.ui.auth.LoginScreen
import com.example.catcom.ui.auth.RegisterScreen
import com.example.catcom.ui.chat.ChatDetailScreen
import com.example.catcom.ui.chat.ChatListScreen
import com.example.catcom.ui.comment.CommentScreen
import com.example.catcom.ui.feed.CreatePostScreen
import com.example.catcom.ui.feed.FeedScreen
import com.example.catcom.ui.feed.FeedViewModel
import com.example.catcom.ui.profile.ProfileScreen
import com.example.catcom.ui.search.SearchScreen
import com.example.catcom.ui.theme.CatcomTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatcomTheme {
                val navController = rememberNavController()
                
                // Tentukan startDestination berdasarkan status login user saat ini
                val startDestination = if (auth.currentUser != null) "feed" else "login"

                // Cek route saat ini untuk menentukan visibilitas BottomBar
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val mainRoutes = listOf("feed", "adoption_list", "profile/{userId}")
                
                // Logic khusus untuk highlight tab Profil di BottomBar
                val isProfileRoute = currentRoute?.startsWith("profile/") == true
                val showBottomBar = currentRoute in listOf("feed", "adoption_list") || isProfileRoute

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                // Item Feed
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = "Feed") },
                                    label = { Text("Feed") },
                                    selected = currentRoute == "feed",
                                    onClick = {
                                        navController.navigate("feed") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                // Item Adoption
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Pets, contentDescription = "Adopsi") },
                                    label = { Text("Adopsi") },
                                    selected = currentRoute == "adoption_list",
                                    onClick = {
                                        navController.navigate("adoption_list") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                // Item Profile
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                                    label = { Text("Profil") },
                                    selected = isProfileRoute,
                                    onClick = {
                                        navController.navigate("profile/me") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- Auth Routes ---
                        composable("login") {
                            LoginScreen(
                                onNavigateToHome = {
                                    navController.navigate("feed") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onNavigateToHome = {
                                    navController.navigate("feed") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- Feed Routes ---
                        composable("feed") {
                            val viewModel = hiltViewModel<FeedViewModel>()
                            FeedScreen(
                                viewModel = viewModel,
                                onNavigateToCreatePost = {
                                    navController.navigate("create_post")
                                },
                                onNavigateToComment = { postId ->
                                    navController.navigate("comment/$postId")
                                },
                                onNavigateToInbox = {
                                    navController.navigate("inbox")
                                },
                                onNavigateToChat = { targetUserId ->
                                    navController.navigate("chat/$targetUserId")
                                },
                                onNavigateToSearch = {
                                    navController.navigate("search")
                                }
                            )
                        }

                        composable("create_post") {
                            val viewModel = hiltViewModel<FeedViewModel>()
                            CreatePostScreen(
                                viewModel = viewModel,
                                onPostCreated = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Route untuk Komentar
                        composable("comment/{postId}") {
                            CommentScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Route Search
                        composable("search") {
                            SearchScreen(
                                onNavigateToProfile = { userId ->
                                    navController.navigate("profile/$userId")
                                },
                                onNavigateToPostDetail = { postId ->
                                    navController.navigate("comment/$postId") // Navigasi ke detail/komentar
                                }
                            )
                        }

                        // --- Chat Routes ---
                        composable("inbox") {
                            ChatListScreen(
                                onNavigateToChat = { targetUserId, _ ->
                                    navController.navigate("chat/$targetUserId")
                                }
                            )
                        }

                        composable("chat/{targetUserId}") { backStackEntry ->
                            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
                            ChatDetailScreen(
                                targetUserId = targetUserId,
                                targetUserName = "Chat", // Placeholder
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- Adoption Routes ---
                        composable("adoption_list") {
                            val viewModel = hiltViewModel<AdoptionViewModel>()
                            AdoptionListScreen(
                                viewModel = viewModel,
                                onNavigateToForm = {
                                    navController.navigate("adoption_form")
                                }
                            )
                        }
                        
                        composable("adoption_form") {
                            val viewModel = hiltViewModel<AdoptionViewModel>()
                            AdoptionFormScreen(
                                viewModel = viewModel,
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- Profile Routes ---
                        composable("profile/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: "me"
                            ProfileScreen(
                                userId = userId,
                                onNavigateToChat = { targetUserId ->
                                    navController.navigate("chat/$targetUserId")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("feed") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
