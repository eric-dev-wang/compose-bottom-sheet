// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "BottomSheetSPM",
    platforms: [.iOS(.v16)],
    products: [
        .library(
            name: "BottomSheetSPM",
            type: .static,
            targets: ["BottomSheetSPM"]
        )
    ],
    targets: [
        .target(
            name: "BottomSheetSPM",
            dependencies: [],
            path: "Sources/BottomSheetSPM"
        )
    ]
)
