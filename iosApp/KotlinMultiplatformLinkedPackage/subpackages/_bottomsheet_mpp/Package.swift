// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_bottomsheet_mpp",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_bottomsheet_mpp",
      type: .none,
      targets: ["_bottomsheet_mpp"]
    )
  ],
  dependencies: [
    .package(
      path: "../../../../bottomsheet-spm"
    )
  ],
  targets: [
    .target(
      name: "_bottomsheet_mpp",
      dependencies: [
        .product(
          name: "BottomSheetSPM",
          package: "bottomsheet-spm"
        )
      ]
    )
  ]
)
