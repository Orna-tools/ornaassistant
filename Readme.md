# Orna Assistant

  **A modern companion app for Orna RPG players**
  
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  [![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
  [![Build Status](https://github.com/Orna-tools/ornaassistant/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/Orna-tools/ornaassistant/actions/workflows/android.yml)
  
  *Enhance your Orna RPG experience with intelligent overlays, automatic tracking, and comprehensive statistics*
</div>

## Overview

Orna Assistant is a powerful Android accessibility service that seamlessly integrates with Orna RPG to provide real-time gameplay enhancements. Track your adventures, optimize your runs, and never miss important game events with intelligent overlays and automated monitoring.

### System Requirements

- **Android**: 8.0 (API level 26) or higher
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 50MB free space
- **Permissions**: Accessibility service, overlay display

## Features

### 🏰 Advanced Dungeon Analytics
- **Smart Tracking**: Automatically monitors dungeon visits with comprehensive statistics
- **Detailed Metrics**: Records orns, gold, experience, floor progression, and completion times
- **Multi-Mode Support**: Covers Normal, Hard, Boss, and Endless dungeon modes
- **Godforge Detection**: Tracks rare godforge drops and their frequency
- **Historical Data**: Maintains complete dungeon run history with search and filtering

### ⚓ Wayvessel Management
- **Session Monitoring**: Tracks wayvessel activation and cumulative session rewards
- **Intelligent Grouping**: Automatically groups dungeons by wayvessel sessions
- **Performance Insights**: Analyzes wayvessel efficiency and optimal usage patterns

### 🎯 Dynamic Overlay System
- **Session Display**: Real-time dungeon and wayvessel statistics overlay
- **Party Integration**: Shows party invites with dungeon cooldown information
- **Item Assessment**: Instant item evaluation using orna.guide API integration
- **Customizable UI**: Fully draggable overlays with adjustable transparency
- **Smart Positioning**: Overlay positions automatically saved per game screen

### 📈 Comprehensive Analytics
- **Visual Reports**: Weekly dungeon visit charts and trend analysis
- **Performance Metrics**: Completion rates, favorite modes, and average durations
- **Data Export**: Export functionality for external analysis and backup
- **Filtering Options**: Advanced search and filter capabilities for historical data

## Screenshots

> 📸 Screenshots coming soon! We're preparing beautiful examples of the app in action.

## Installation

### Download & Install

1. **Download**: Get the latest APK from our [Releases](https://github.com/Orna-tools/ornaassistant/releases) page
2. **Enable Unknown Sources**:
   - Android 7-8: Settings → Security → Unknown Sources
   - Android 9+: Settings → Apps & Notifications → Advanced → Special app access → Install unknown apps
3. **Install**: Tap the downloaded APK to install
4. **Launch**: Open Orna Assistant from your app drawer

### Required Permissions

#### ✅ Accessibility Service (Required)
```
Settings → Accessibility → Orna Assistant → Enable
```

#### ✅ Display Overlay (Required)  
```
Settings → Apps → Orna Assistant → Advanced → Display over other apps → Enable
```

#### 🔔 Notifications (Optional)
- Allow for background service status updates

## Quick Start

1. **Setup**: Launch Orna Assistant and grant required permissions
2. **Customize**: Configure overlay preferences in settings
3. **Play**: Start Orna RPG - overlays will appear automatically
4. **Track**: View your statistics and history in the app dashboard

### Using Overlays

| Action | Result |
|--------|---------|
| **Single tap** | Dismiss overlay temporarily |
| **Long press + drag** | Reposition overlay |
| **Settings menu** | Toggle overlays and adjust transparency |

## Configuration

### Overlay Settings
- **Transparency**: Adjust opacity (10-100%)
- **Position Lock**: Prevent accidental repositioning  
- **Auto-hide**: Configure timeout settings
- **Scale Factor**: Resize for different screen sizes

### Data Management
- **Export Options**: CSV, JSON formats for external analysis
- **Backup Settings**: Cloud sync and local backup configurations
- **Data Retention**: Automatic cleanup of old records
- **Privacy Controls**: Manage data sharing preferences

## Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| **Overlays not appearing** | Verify overlay permission is granted and Orna is running |
| **Tracking not working** | Ensure accessibility service is enabled and active |
| **Performance issues** | Close unnecessary apps and restart both Orna and Assistant |

### Still Having Issues?

Check our [Issues](https://github.com/Orna-tools/ornaassistant/issues) page or contact support below.

## Contributing

We welcome contributions from the community! 

### Getting Started

```bash
# Clone the repository
git clone https://github.com/Orna-tools/ornaassistant.git
cd ornaassistant

# Build the project
./gradlew build
```

### How to Contribute

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

Please read our [Contributing Guidelines](CONTRIBUTING.md) for detailed information.

### Development

- **Language**: Kotlin 2.0.20
- **Min SDK**: Android 8.0 (API 26)
- **Architecture**: MVVM with Android Architecture Components
- **Build System**: Gradle with Kotlin DSL

## Support

### Get Help

- 📧 **Email**: [support@ornaassistant.com](mailto:support@ornaassistant.com)
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/Orna-tools/ornaassistant/issues)
- 💬 **Community**: [Discord Server](https://discord.gg/dFgCXK2vNR)

### Feedback

Your feedback helps us improve! Please consider:
- ⭐ **Starring** this repository
- 🐛 **Reporting** bugs and issues
- 💡 **Suggesting** new features
- 📝 **Contributing** to documentation

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- 🎮 The Orna RPG community for feedback and feature requests
- 🔗 [orna.guide](https://orna.guide) for providing the item assessment API
- 👥 All contributors and beta testers who make this project possible

---

<div align="center">
  <strong>Made with ❤️ for the Orna RPG community</strong>
  
  If Orna Assistant enhances your gameplay, please ⭐ star this repository!
</div>
