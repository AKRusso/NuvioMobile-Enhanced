import UIKit
import SwiftUI
import ComposeApp
import ImageIO

private enum NuvioNativeTabIcon {
    struct ProfileAvatarAnimation {
        let frames: [UIImage]
        let duration: TimeInterval
    }

    static let home = vectorIcon(
        viewport: CGSize(width: 24, height: 24),
        paths: [
            "M10,20V14H14V20H19V12H22L12,3L2,12H5V20Z",
        ]
    )

    static let search = drawnIcon { context, rect in
        drawInViewport(context: context, rect: rect, viewport: CGSize(width: 20, height: 20)) {
            context.setStrokeColor(UIColor.black.cgColor)
            context.setLineWidth(2)
            context.setLineCap(.round)
            context.strokeEllipse(in: CGRect(x: 3, y: 3, width: 12, height: 12))
            context.move(to: CGPoint(x: 13.6, y: 13.6))
            context.addLine(to: CGPoint(x: 17, y: 17))
            context.strokePath()
        }
    }

    static let liveTv = symbolIcon(systemName: "tv")

    static let library = vectorIcon(
        viewport: CGSize(width: 24, height: 24),
        paths: [
            "M8.50989,2.00001H15.49C15.7225,1.99995 15.9007,1.99991 16.0565,2.01515C17.1643,2.12352 18.0711,2.78958 18.4556,3.68678H5.54428C5.92879,2.78958 6.83555,2.12352 7.94337,2.01515C8.09917,1.99991 8.27741,1.99995 8.50989,2.00001Z",
            "M6.31052,4.72312C4.91989,4.72312 3.77963,5.56287 3.3991,6.67691C3.39117,6.70013 3.38356,6.72348 3.37629,6.74693C3.77444,6.62636 4.18881,6.54759 4.60827,6.49382C5.68865,6.35531 7.05399,6.35538 8.64002,6.35547L8.75846,6.35547L15.5321,6.35547C17.1181,6.35538 18.4835,6.35531 19.5639,6.49382C19.9833,6.54759 20.3977,6.62636 20.7958,6.74693C20.7886,6.72348 20.781,6.70013 20.773,6.67691C20.3925,5.56287 19.2522,4.72312 17.8616,4.72312H6.31052Z",
            "M8.67239,7.54204H15.3276C18.7024,7.54204 20.3898,7.54204 21.3377,8.52887C22.2855,9.5157 22.0625,11.0403 21.6165,14.0896L21.1935,16.9811C20.8437,19.3724 20.6689,20.568 19.7717,21.284C18.8745,22 17.5512,22 14.9046,22H9.09536C6.44881,22 5.12553,22 4.22834,21.284C3.33115,20.568 3.15626,19.3724 2.80648,16.9811L2.38351,14.0896C1.93748,11.0403 1.71447,9.5157 2.66232,8.52887C3.61017,7.54204 5.29758,7.54204 8.67239,7.54204ZM8,18.0001C8,17.5859 8.3731,17.2501 8.83333,17.2501H15.1667C15.6269,17.2501 16,17.5859 16,18.0001C16,18.4144 15.6269,18.7502 15.1667,18.7502H8.83333C8.3731,18.7502 8,18.4144 8,18.0001Z",
        ]
    )

    static let profileFallback = vectorIcon(
        viewport: CGSize(width: 24, height: 24),
        paths: [
            "M12,12C14.21,12 16,10.21 16,8C16,5.79 14.21,4 12,4C9.79,4 8,5.79 8,8C8,10.21 9.79,12 12,12ZM12,14C9.33,14 4,15.34 4,18V19C4,19.55 4.45,20 5,20H19C19.55,20 20,19.55 20,19V18C20,15.34 14.67,14 12,14Z",
        ]
    )

    static func profileAvatar(
        name: String?,
        avatarColor: UIColor?,
        backgroundColor: UIColor?,
        avatarImage: UIImage?,
        selected: Bool,
        accent: UIColor
    ) -> UIImage {
        guard name != nil || avatarColor != nil || avatarImage != nil else {
            return profileFallback
        }

        let size = CGSize(width: 28, height: 28)
        let baseColor = avatarColor ?? UIColor(red: 30.0 / 255.0, green: 136.0 / 255.0, blue: 229.0 / 255.0, alpha: 1)
        let fillColor = backgroundColor ?? baseColor.withAlphaComponent(0.15)
        let borderColor = selected ? accent : baseColor.withAlphaComponent(0.5)
        let initial = name?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .prefix(1)
            .uppercased() ?? ""

        return renderProfileAvatarFrame(
            size: size,
            fillColor: fillColor,
            borderColor: borderColor,
            baseColor: baseColor,
            initial: initial,
            avatarImage: avatarImage
        ).withRenderingMode(.alwaysOriginal)
    }

    static func profileAvatarAnimation(
        name: String?,
        avatarColor: UIColor?,
        backgroundColor: UIColor?,
        avatarFrames: [UIImage],
        avatarDuration: TimeInterval,
        selected: Bool,
        accent: UIColor
    ) -> ProfileAvatarAnimation? {
        guard avatarFrames.count > 1 else {
            return nil
        }

        let size = CGSize(width: 28, height: 28)
        let baseColor = avatarColor ?? UIColor(red: 30.0 / 255.0, green: 136.0 / 255.0, blue: 229.0 / 255.0, alpha: 1)
        let fillColor = backgroundColor ?? baseColor.withAlphaComponent(0.15)
        let borderColor = selected ? accent : baseColor.withAlphaComponent(0.5)
        let initial = name?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .prefix(1)
            .uppercased() ?? ""
        let frames = avatarFrames.map { frame in
            renderProfileAvatarFrame(
                size: size,
                fillColor: fillColor,
                borderColor: borderColor,
                baseColor: baseColor,
                initial: initial,
                avatarImage: frame
            ).withRenderingMode(.alwaysOriginal)
        }
        let fallbackDuration = Double(frames.count) * 0.1
        let duration = max(avatarDuration > 0 ? avatarDuration : fallbackDuration, Double(frames.count) * 0.02)
        return ProfileAvatarAnimation(frames: frames, duration: duration)
    }

    private static func renderProfileAvatarFrame(
        size: CGSize,
        fillColor: UIColor,
        borderColor: UIColor,
        baseColor: UIColor,
        initial: String,
        avatarImage: UIImage?
    ) -> UIImage {
        UIGraphicsImageRenderer(size: size).image { _ in
            let rect = CGRect(origin: .zero, size: size).insetBy(dx: 1, dy: 1)
            fillColor.setFill()
            UIBezierPath(ovalIn: rect).fill()

            if let avatarImage {
                UIBezierPath(ovalIn: rect).addClip()
                drawAspectFill(image: avatarImage, in: rect)
            } else if !initial.isEmpty {
                let font = UIFont.systemFont(ofSize: size.height * 0.45, weight: .bold)
                let attributes: [NSAttributedString.Key: Any] = [
                    .font: font,
                    .foregroundColor: baseColor,
                ]
                let textSize = initial.size(withAttributes: attributes)
                initial.draw(
                    at: CGPoint(
                        x: rect.midX - textSize.width / 2,
                        y: rect.midY - textSize.height / 2
                    ),
                    withAttributes: attributes
                )
            } else {
                profileFallback
                    .withTintColor(baseColor, renderingMode: .alwaysOriginal)
                    .draw(in: rect.insetBy(dx: 5.5, dy: 5.5))
            }

            borderColor.setStroke()
            let borderPath = UIBezierPath(ovalIn: rect.insetBy(dx: 0.75, dy: 0.75))
            borderPath.lineWidth = 1.5
            borderPath.stroke()
        }
    }

    private static func drawInViewport(
        context: CGContext,
        rect: CGRect,
        viewport: CGSize,
        draw: () -> Void
    ) {
        let scale = min(rect.width / viewport.width, rect.height / viewport.height)
        let x = rect.midX - viewport.width * scale / 2
        let y = rect.midY - viewport.height * scale / 2
        context.saveGState()
        context.translateBy(x: x, y: y)
        context.scaleBy(x: scale, y: scale)
        draw()
        context.restoreGState()
    }

    private static func vectorIcon(viewport: CGSize, paths: [String], size: CGSize = CGSize(width: 25, height: 25)) -> UIImage {
        drawnIcon(size: size) { context, rect in
            drawInViewport(context: context, rect: rect, viewport: viewport) {
                context.setFillColor(UIColor.black.cgColor)
                paths.compactMap { SVGPath(data: $0).cgPath }.forEach { path in
                    context.addPath(path)
                    context.fillPath(using: .evenOdd)
                }
            }
        }
    }

    private static func symbolIcon(systemName: String, size: CGSize = CGSize(width: 25, height: 25)) -> UIImage {
        let image = UIImage(
            systemName: systemName,
            withConfiguration: UIImage.SymbolConfiguration(pointSize: 20, weight: .regular)
        ) ?? UIImage(systemName: systemName) ?? profileFallback
        return image.withRenderingMode(.alwaysTemplate)
    }

    private static func drawnIcon(
        size: CGSize = CGSize(width: 25, height: 25),
        draw: @escaping (CGContext, CGRect) -> Void
    ) -> UIImage {
        UIGraphicsImageRenderer(size: size).image { rendererContext in
            draw(rendererContext.cgContext, CGRect(origin: .zero, size: size))
        }.withRenderingMode(.alwaysTemplate)
    }

    private static func drawAspectFill(image: UIImage, in rect: CGRect) {
        guard image.size.width > 0, image.size.height > 0 else { return }
        let scale = max(rect.width / image.size.width, rect.height / image.size.height)
        let drawSize = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let drawRect = CGRect(
            x: rect.midX - drawSize.width / 2,
            y: rect.midY - drawSize.height / 2,
            width: drawSize.width,
            height: drawSize.height
        )
        image.draw(in: drawRect)
    }

    private struct SVGPath {
        private enum Token {
            case command(Character)
            case number(CGFloat)
        }

        let data: String

        var cgPath: CGPath? {
            let tokens = Self.tokens(from: data)
            var index = 0
            var command: Character?
            var current = CGPoint.zero
            var subpathStart = CGPoint.zero
            let path = CGMutablePath()

            func hasNumber() -> Bool {
                guard index < tokens.count else { return false }
                if case .number = tokens[index] { return true }
                return false
            }

            func readNumber() -> CGFloat? {
                guard index < tokens.count else { return nil }
                guard case let .number(value) = tokens[index] else { return nil }
                index += 1
                return value
            }

            func readPoint(relative: Bool) -> CGPoint? {
                guard let x = readNumber(), let y = readNumber() else { return nil }
                let point = CGPoint(x: x, y: y)
                return relative ? CGPoint(x: current.x + point.x, y: current.y + point.y) : point
            }

            while index < tokens.count {
                if case let .command(value) = tokens[index] {
                    command = value
                    index += 1
                }

                guard let activeCommand = command else { return nil }
                let relative = activeCommand.isLowercase

                switch activeCommand.uppercased() {
                case "M":
                    guard let point = readPoint(relative: relative) else { return nil }
                    path.move(to: point)
                    current = point
                    subpathStart = point
                    command = relative ? "l" : "L"
                case "L":
                    while hasNumber() {
                        guard let point = readPoint(relative: relative) else { return nil }
                        path.addLine(to: point)
                        current = point
                    }
                case "H":
                    while hasNumber() {
                        guard let x = readNumber() else { return nil }
                        let point = CGPoint(x: relative ? current.x + x : x, y: current.y)
                        path.addLine(to: point)
                        current = point
                    }
                case "V":
                    while hasNumber() {
                        guard let y = readNumber() else { return nil }
                        let point = CGPoint(x: current.x, y: relative ? current.y + y : y)
                        path.addLine(to: point)
                        current = point
                    }
                case "C":
                    while hasNumber() {
                        guard
                            let c1 = readPoint(relative: relative),
                            let c2 = readPoint(relative: relative),
                            let end = readPoint(relative: relative)
                        else { return nil }
                        path.addCurve(to: end, control1: c1, control2: c2)
                        current = end
                    }
                case "Z":
                    path.closeSubpath()
                    current = subpathStart
                default:
                    return nil
                }
            }

            return path
        }

        private static func tokens(from data: String) -> [Token] {
            let pattern = "[MmLlHhVvCcZz]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?"
            guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
            let range = NSRange(data.startIndex..<data.endIndex, in: data)
            return regex.matches(in: data, range: range).compactMap { match in
                guard let tokenRange = Range(match.range, in: data) else { return nil }
                let token = String(data[tokenRange])
                if token.count == 1, let character = token.first, character.isLetter {
                    return .command(character)
                }
                guard let value = Double(token) else { return nil }
                return .number(CGFloat(value))
            }
        }
    }
}

final class RootComposeViewController: UIViewController, UITabBarDelegate {
    private struct ProfileAvatarImagePayload {
        let image: UIImage
        let animationFrames: [UIImage]
        let animationDuration: TimeInterval
    }

    private enum NativeTab: String, CaseIterable {
        case home = "Home"
        case search = "Search"
        case liveTv = "LiveTv"
        case library = "Library"
        case settings = "Settings"

        var tag: Int {
            switch self {
            case .home: return 0
            case .search: return 1
            case .liveTv: return 2
            case .library: return 3
            case .settings: return 4
            }
        }

        var titleKey: String {
            switch self {
            case .home: return "NuvioNativeTabTitleHome"
            case .search: return "NuvioNativeTabTitleSearch"
            case .liveTv: return "NuvioNativeTabTitleLiveTv"
            case .library: return "NuvioNativeTabTitleLibrary"
            case .settings: return "NuvioNativeTabTitleProfile"
            }
        }

        var fallbackTitle: String {
            switch self {
            case .home: return "Home"
            case .search: return "Search"
            case .liveTv: return "Live TV"
            case .library: return "Library"
            case .settings: return "Profile"
            }
        }

        func localizedTitle(defaults: UserDefaults = .standard) -> String {
            defaults.string(forKey: titleKey)?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? fallbackTitle
        }

        var iconImage: UIImage {
            switch self {
            case .home: return NuvioNativeTabIcon.home
            case .search: return NuvioNativeTabIcon.search
            case .liveTv: return NuvioNativeTabIcon.liveTv
            case .library: return NuvioNativeTabIcon.library
            case .settings: return NuvioNativeTabIcon.profileFallback
            }
        }

        init?(tag: Int) {
            guard let tab = Self.allCases.first(where: { $0.tag == tag }) else { return nil }
            self = tab
        }
    }

    private static let liquidGlassEnabledKey = "NuvioLiquidGlassNativeTabBarEnabled"
    private static let nativeTabBarVisibleKey = "NuvioNativeTabBarVisible"
    private static let nativeLiveTvEnabledKey = "NuvioNativeLiveTvEnabled"
    private static let nativeSelectedTabKey = "NuvioNativeSelectedTab"
    private static let nativeTabAccentColorKey = "NuvioNativeTabAccentColor"
    private static let nativeProfileNameKey = "NuvioNativeProfileName"
    private static let nativeProfileAvatarColorKey = "NuvioNativeProfileAvatarColor"
    private static let nativeProfileAvatarURLKey = "NuvioNativeProfileAvatarURL"
    private static let nativeProfileAvatarBackgroundColorKey = "NuvioNativeProfileAvatarBackgroundColor"
    private static let nativeTabChromeDidChangeNotification = Notification.Name("NuvioNativeTabChromeDidChange")

    private let contentController: UIViewController
    private let tabBar = UITabBar()
    private let profileTabTouchOverlay = UIControl()
    private let profileTabAvatarAnimationView = UIImageView()
    private var contentBottomToViewBottom: NSLayoutConstraint?
    private var tabBarHeightConstraint: NSLayoutConstraint?
    private var tabChromeObserver: NSObjectProtocol?
    private var tabChromeSyncWorkItem: DispatchWorkItem?
    private var profileTouchRestoreTab: NativeTab?
    private var profileLongPressHandled = false
    private var profileAvatarImageURL: String?
    private var profileAvatarImageTask: URLSessionDataTask?
    private var profileAvatarImage: UIImage?
    private var profileAvatarAnimationSourceFrames: [UIImage] = []
    private var profileAvatarAnimationSourceDuration: TimeInterval = 0
    private var profileTabAvatarAnimationKey: String?
    private var profileTabAvatarAnimationFrames: [UIImage] = []
    private var profileTabAvatarAnimationFrameIndex = 0
    private var profileTabAvatarAnimationFrameDelay: TimeInterval = 0.1
    private var profileTabAvatarAnimationTimer: Timer?
    private weak var profileTabStaticImageView: UIImageView?

    init(contentController: UIViewController) {
        self.contentController = contentController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .black
        contentController.view.backgroundColor = .black
        UserDefaults.standard.set(false, forKey: Self.nativeTabBarVisibleKey)

        addChild(contentController)
        view.addSubview(contentController.view)
        contentController.view.translatesAutoresizingMaskIntoConstraints = false
        let bottomToViewBottom = contentController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        self.contentBottomToViewBottom = bottomToViewBottom
        NSLayoutConstraint.activate([
            contentController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentController.view.topAnchor.constraint(equalTo: view.topAnchor),
            bottomToViewBottom,
        ])
        contentController.didMove(toParent: self)

        configureNativeTabBar()
        installNativeTabObservers()
        syncNativeTabChrome(animated: false)
    }

    deinit {
        if let tabChromeObserver {
            NotificationCenter.default.removeObserver(tabChromeObserver)
        }
        tabChromeSyncWorkItem?.cancel()
        profileAvatarImageTask?.cancel()
        profileTabAvatarAnimationTimer?.invalidate()
        clearProfileTabAvatarAnimation()
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateTabBarHeight()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateProfileTabTouchOverlayFrame()
    }

    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        guard let tab = NativeTab(tag: item.tag) else { return }
        selectNativeTab(tab)
    }

    override var childForHomeIndicatorAutoHidden: UIViewController? {
        immersiveController(in: contentController) ?? contentController
    }

    override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        immersiveController(in: contentController) ?? contentController
    }

    override var childForStatusBarHidden: UIViewController? {
        immersiveController(in: contentController) ?? contentController
    }

    override var prefersHomeIndicatorAutoHidden: Bool {
        immersiveController(in: contentController)?.prefersHomeIndicatorAutoHidden ?? false
    }

    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge {
        immersiveController(in: contentController)?.preferredScreenEdgesDeferringSystemGestures ?? []
    }

    override var prefersStatusBarHidden: Bool {
        immersiveController(in: contentController)?.prefersStatusBarHidden ?? false
    }

    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation {
        .fade
    }

    func refreshImmersiveSystemUI() {
        setNeedsUpdateOfHomeIndicatorAutoHidden()
        setNeedsUpdateOfScreenEdgesDeferringSystemGestures()
        setNeedsStatusBarAppearanceUpdate()
    }

    private func immersiveController(in controller: UIViewController?) -> UIViewController? {
        guard let controller else { return nil }

        if controller.prefersHomeIndicatorAutoHidden ||
            !controller.preferredScreenEdgesDeferringSystemGestures.isEmpty ||
            controller.prefersStatusBarHidden {
            return controller
        }

        if let presented = immersiveController(in: controller.presentedViewController) {
            return presented
        }

        for child in controller.children.reversed() {
            if let immersiveChild = immersiveController(in: child) {
                return immersiveChild
            }
        }

        return nil
    }

    private var nativeTabsSupported: Bool {
        UIDevice.current.userInterfaceIdiom == .phone &&
            ProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 26
    }

    private var shouldShowNativeTabBar: Bool {
        nativeTabsSupported &&
            UserDefaults.standard.bool(forKey: Self.liquidGlassEnabledKey) &&
            UserDefaults.standard.bool(forKey: Self.nativeTabBarVisibleKey)
    }

    private var liveTvTabEnabled: Bool {
        let defaults = UserDefaults.standard
        if defaults.object(forKey: Self.nativeLiveTvEnabledKey) == nil {
            return true
        }
        return defaults.bool(forKey: Self.nativeLiveTvEnabledKey)
    }

    private var visibleNativeTabs: [NativeTab] {
        liveTvTabEnabled ? NativeTab.allCases : NativeTab.allCases.filter { $0 != .liveTv }
    }

    private func configureNativeTabBar() {
        tabBar.delegate = self
        tabBar.translatesAutoresizingMaskIntoConstraints = false
        tabBar.items = makeNativeTabItems(for: visibleNativeTabs)
        tabBar.selectedItem = tabBar.items?.first
        applyNativeTabBarAppearance()
        tabBar.alpha = 0
        tabBar.isHidden = true

        view.addSubview(tabBar)
        configureProfileTabTouchOverlay()
        let heightConstraint = tabBar.heightAnchor.constraint(equalToConstant: tabBarHeight)
        tabBarHeightConstraint = heightConstraint
        NSLayoutConstraint.activate([
            tabBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tabBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tabBar.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            heightConstraint,
        ])
    }

    private func installNativeTabObservers() {
        tabChromeObserver = NotificationCenter.default.addObserver(
            forName: Self.nativeTabChromeDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.scheduleNativeTabChromeSync(animated: true)
        }
    }

    private var tabBarHeight: CGFloat {
        49 + view.safeAreaInsets.bottom
    }

    private func updateTabBarHeight() {
        tabBarHeightConstraint?.constant = tabBarHeight
        updateProfileTabTouchOverlayFrame()
    }

    private func scheduleNativeTabChromeSync(animated: Bool) {
        tabChromeSyncWorkItem?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            self?.syncNativeTabChrome(animated: animated)
        }
        tabChromeSyncWorkItem = workItem
        DispatchQueue.main.async(execute: workItem)
    }

    private func syncNativeTabChrome(animated: Bool) {
        updateTabBarHeight()
        syncNativeTabItems()
        applyNativeTabBarAppearance()
        syncSelectedNativeTab()
        updateProfileTabAvatarAnimation()

        let visible = shouldShowNativeTabBar
        contentBottomToViewBottom?.isActive = true
        if visible {
            tabBar.isHidden = false
            profileTabTouchOverlay.isHidden = false
            profileTabAvatarAnimationView.isHidden = profileTabAvatarAnimationView.image == nil
        }

        let changes = {
            self.tabBar.alpha = visible ? 1 : 0
            self.profileTabTouchOverlay.alpha = visible ? 1 : 0
            self.profileTabAvatarAnimationView.alpha = visible ? 1 : 0
            self.view.layoutIfNeeded()
            self.view.bringSubviewToFront(self.profileTabAvatarAnimationView)
            self.view.bringSubviewToFront(self.profileTabTouchOverlay)
        }

        let completion: (Bool) -> Void = { _ in
            self.tabBar.isHidden = !visible
            self.profileTabTouchOverlay.isHidden = !visible
            self.syncProfileTabAvatarAnimationVisibility()
        }

        if animated && view.window != nil {
            UIView.animate(
                withDuration: 0.22,
                delay: 0,
                options: [.beginFromCurrentState, .curveEaseInOut],
                animations: changes,
                completion: completion
            )
        } else {
            changes()
            completion(true)
        }
    }

    private func syncSelectedNativeTab() {
        tabBar.selectedItem = tabBar.items?.first(where: { $0.tag == currentNativeSelectedTab.tag })
        updateProfileTabAvatarAnimation()
    }

    private func makeNativeTabItems(for tabs: [NativeTab]) -> [UITabBarItem] {
        tabs.map { tab in
            let item = UITabBarItem(
                title: tab.localizedTitle(),
                image: tab.iconImage,
                selectedImage: tab.iconImage
            )
            item.tag = tab.tag
            return item
        }
    }

    private func syncNativeTabItems() {
        let tabs = visibleNativeTabs
        let currentTags = tabBar.items?.map(\.tag) ?? []
        let expectedTags = tabs.map(\.tag)
        guard currentTags != expectedTags else { return }

        tabBar.items = makeNativeTabItems(for: tabs)
        let rawSelectedTab = UserDefaults.standard.string(forKey: Self.nativeSelectedTabKey)
        if rawSelectedTab == NativeTab.liveTv.rawValue && !liveTvTabEnabled {
            UserDefaults.standard.set(NativeTab.home.rawValue, forKey: Self.nativeSelectedTabKey)
            DispatchQueue.main.async {
                NativeTabBridgeKt.nativeTabSelect(tabName: NativeTab.home.rawValue)
            }
        }
        updateProfileTabTouchOverlayFrame()
    }

    @objc private func handleNativeProfileTabLongPress(_ recognizer: UILongPressGestureRecognizer) {
        guard recognizer.state == .began else { return }

        profileLongPressHandled = true
        DispatchQueue.main.async {
            NativeTabBridgeKt.nativeProfileTabLongPress()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) { [weak self] in
            self?.restoreProfileTabTouchIfNeeded()
        }
    }

    @objc private func handleNativeProfileTabTouchDown() {
        profileTouchRestoreTab = currentNativeSelectedTab
        profileLongPressHandled = false
    }

    @objc private func handleNativeProfileTabTap() {
        if profileLongPressHandled {
            profileLongPressHandled = false
            restoreProfileTabTouchIfNeeded()
            return
        }
        profileTouchRestoreTab = nil
        selectNativeTab(.settings)
    }

    @objc private func handleNativeProfileTabTouchCancel() {
        profileLongPressHandled = false
        restoreProfileTabTouchIfNeeded()
    }

    private var currentNativeSelectedTab: NativeTab {
        let rawValue = UserDefaults.standard.string(forKey: Self.nativeSelectedTabKey) ?? NativeTab.home.rawValue
        let tab = NativeTab(rawValue: rawValue) ?? .home
        if tab == .liveTv && !liveTvTabEnabled {
            return .home
        }
        return tab
    }

    private func selectNativeTab(_ tab: NativeTab) {
        guard visibleNativeTabs.contains(tab) else { return }
        tabBar.selectedItem = tabBar.items?.first(where: { $0.tag == tab.tag })
        UserDefaults.standard.set(tab.rawValue, forKey: Self.nativeSelectedTabKey)
        updateProfileTabAvatarAnimation()
        NativeTabBridgeKt.nativeTabSelect(tabName: tab.rawValue)
    }

    private func configureProfileTabTouchOverlay() {
        profileTabAvatarAnimationView.contentMode = .scaleAspectFill
        profileTabAvatarAnimationView.clipsToBounds = false
        profileTabAvatarAnimationView.isUserInteractionEnabled = false
        profileTabAvatarAnimationView.alpha = 0
        profileTabAvatarAnimationView.isHidden = true
        profileTabAvatarAnimationView.layer.zPosition = 1000
        view.addSubview(profileTabAvatarAnimationView)

        profileTabTouchOverlay.backgroundColor = .clear
        profileTabTouchOverlay.isOpaque = false
        profileTabTouchOverlay.isExclusiveTouch = true
        profileTabTouchOverlay.accessibilityLabel = NativeTab.settings.localizedTitle()
        profileTabTouchOverlay.accessibilityTraits = .button
        profileTabTouchOverlay.addTarget(
            self,
            action: #selector(handleNativeProfileTabTouchDown),
            for: .touchDown
        )
        profileTabTouchOverlay.addTarget(
            self,
            action: #selector(handleNativeProfileTabTap),
            for: .touchUpInside
        )
        profileTabTouchOverlay.addTarget(
            self,
            action: #selector(handleNativeProfileTabTouchCancel),
            for: [.touchCancel, .touchUpOutside]
        )

        let longPressRecognizer = UILongPressGestureRecognizer(
            target: self,
            action: #selector(handleNativeProfileTabLongPress(_:))
        )
        longPressRecognizer.minimumPressDuration = 0.45
        longPressRecognizer.cancelsTouchesInView = true
        profileTabTouchOverlay.addGestureRecognizer(longPressRecognizer)

        profileTabTouchOverlay.alpha = 0
        profileTabTouchOverlay.isHidden = true
        view.addSubview(profileTabTouchOverlay)
        updateProfileTabTouchOverlayFrame()
    }

    private func restoreProfileTabTouchIfNeeded() {
        let tab = profileTouchRestoreTab ?? currentNativeSelectedTab
        tabBar.selectedItem = tabBar.items?.first(where: { $0.tag == tab.tag })
        profileTouchRestoreTab = nil
    }

    private func updateProfileTabTouchOverlayFrame() {
        let tabs = visibleNativeTabs
        let tabCount = CGFloat(tabs.count)
        guard tabCount > 0, tabBar.bounds.width > 0 else {
            profileTabTouchOverlay.frame = .zero
            updateProfileTabAvatarAnimationFrame()
            syncProfileTabAvatarAnimationVisibility()
            return
        }
        guard let settingsIndex = tabs.firstIndex(of: .settings) else {
            profileTabTouchOverlay.frame = .zero
            updateProfileTabAvatarAnimationFrame()
            syncProfileTabAvatarAnimationVisibility()
            return
        }

        let itemWidth = tabBar.bounds.width / tabCount
        let settingsVisualIndex = CGFloat(settingsIndex)
        let visualIndex: CGFloat
        if tabBar.effectiveUserInterfaceLayoutDirection == .rightToLeft {
            visualIndex = tabCount - 1 - settingsVisualIndex
        } else {
            visualIndex = settingsVisualIndex
        }
        let overlayFrameInTabBar = CGRect(
            x: itemWidth * visualIndex,
            y: 0,
            width: itemWidth,
            height: tabBar.bounds.height
        )
        profileTabTouchOverlay.frame = tabBar.convert(overlayFrameInTabBar, to: view)
        profileTabTouchOverlay.alpha = tabBar.alpha
        updateProfileTabAvatarAnimationFrame()
        syncProfileTabAvatarAnimationVisibility()
        view.bringSubviewToFront(profileTabAvatarAnimationView)
        view.bringSubviewToFront(profileTabTouchOverlay)
    }

    private var nativeTabAccentColor: UIColor {
        UIColor(hexString: UserDefaults.standard.string(forKey: Self.nativeTabAccentColorKey)) ??
            UIColor(red: 0.96, green: 0.96, blue: 0.96, alpha: 1)
    }

    private func applyNativeTabBarAppearance() {
        let accent = nativeTabAccentColor
        let unselected = UIColor(red: 150 / 255, green: 156 / 255, blue: 163 / 255, alpha: 1)

        updateNativeTabTitles()
        refreshProfileAvatarImageIfNeeded()
        updateNativeTabImages(accent: accent)
        updateProfileTabAvatarAnimation()

        tabBar.tintColor = accent
        tabBar.unselectedItemTintColor = unselected

        let appearance = tabBar.standardAppearance.copy() as! UITabBarAppearance
        appearance.stackedLayoutAppearance.normal.iconColor = unselected
        appearance.stackedLayoutAppearance.normal.titleTextAttributes = [.foregroundColor: unselected]
        appearance.stackedLayoutAppearance.selected.iconColor = accent
        appearance.stackedLayoutAppearance.selected.titleTextAttributes = [.foregroundColor: accent]
        appearance.inlineLayoutAppearance.normal.iconColor = unselected
        appearance.inlineLayoutAppearance.normal.titleTextAttributes = [.foregroundColor: unselected]
        appearance.inlineLayoutAppearance.selected.iconColor = accent
        appearance.inlineLayoutAppearance.selected.titleTextAttributes = [.foregroundColor: accent]
        appearance.compactInlineLayoutAppearance.normal.iconColor = unselected
        appearance.compactInlineLayoutAppearance.normal.titleTextAttributes = [.foregroundColor: unselected]
        appearance.compactInlineLayoutAppearance.selected.iconColor = accent
        appearance.compactInlineLayoutAppearance.selected.titleTextAttributes = [.foregroundColor: accent]
        tabBar.standardAppearance = appearance
        tabBar.scrollEdgeAppearance = appearance
    }

    private func updateNativeTabImages(accent: UIColor) {
        tabBar.items?.forEach { item in
            guard let tab = NativeTab(tag: item.tag) else { return }
            item.image = nativeTabImage(for: tab, selected: false, accent: accent)
            item.selectedImage = nativeTabImage(for: tab, selected: true, accent: accent)
        }
    }

    private func updateNativeTabTitles() {
        tabBar.items?.forEach { item in
            guard let tab = NativeTab(tag: item.tag) else { return }
            item.title = tab.localizedTitle()
        }
        profileTabTouchOverlay.accessibilityLabel = NativeTab.settings.localizedTitle()
    }

    private func nativeTabImage(for tab: NativeTab, selected: Bool, accent: UIColor) -> UIImage {
        guard tab == .settings else {
            return tab.iconImage
        }

        let defaults = UserDefaults.standard
        return NuvioNativeTabIcon.profileAvatar(
            name: defaults.string(forKey: Self.nativeProfileNameKey),
            avatarColor: UIColor(hexString: defaults.string(forKey: Self.nativeProfileAvatarColorKey)),
            backgroundColor: UIColor(hexString: defaults.string(forKey: Self.nativeProfileAvatarBackgroundColorKey)),
            avatarImage: profileAvatarImage,
            selected: selected,
            accent: accent
        )
    }

    private func updateProfileTabAvatarAnimation() {
        let defaults = UserDefaults.standard
        let selected = currentNativeSelectedTab == .settings
        let accent = nativeTabAccentColor
        let animationKey = [
            profileAvatarImageURL ?? "",
            defaults.string(forKey: Self.nativeProfileNameKey) ?? "",
            defaults.string(forKey: Self.nativeProfileAvatarColorKey) ?? "",
            defaults.string(forKey: Self.nativeProfileAvatarBackgroundColorKey) ?? "",
            defaults.string(forKey: Self.nativeTabAccentColorKey) ?? "",
            selected ? "selected" : "normal",
        ].joined(separator: "|")

        guard let avatarAnimation = NuvioNativeTabIcon.profileAvatarAnimation(
            name: defaults.string(forKey: Self.nativeProfileNameKey),
            avatarColor: UIColor(hexString: defaults.string(forKey: Self.nativeProfileAvatarColorKey)),
            backgroundColor: UIColor(hexString: defaults.string(forKey: Self.nativeProfileAvatarBackgroundColorKey)),
            avatarFrames: profileAvatarAnimationSourceFrames,
            avatarDuration: profileAvatarAnimationSourceDuration,
            selected: selected,
            accent: accent
        ) else {
            clearProfileTabAvatarAnimation()
            return
        }

        if profileTabAvatarAnimationKey != animationKey {
            stopProfileTabAvatarAnimationTimer()
            profileTabAvatarAnimationFrames = avatarAnimation.frames
            profileTabAvatarAnimationFrameIndex = 0
            profileTabAvatarAnimationFrameDelay = max(
                avatarAnimation.duration / Double(max(avatarAnimation.frames.count, 1)),
                0.02
            )
            profileTabAvatarAnimationView.animationImages = nil
            profileTabAvatarAnimationView.image = avatarAnimation.frames.first
            if let firstFrame = avatarAnimation.frames.first {
                applyProfileTabAvatarFrame(firstFrame)
            }
            profileTabAvatarAnimationKey = animationKey
        }

        updateProfileTabAvatarAnimationFrame()
        syncProfileTabAvatarAnimationVisibility()
    }

    private func syncProfileTabAvatarAnimationVisibility() {
        let visible = shouldShowNativeTabBar &&
            profileTabAvatarAnimationView.image != nil &&
            !profileTabAvatarAnimationView.frame.isEmpty
        profileTabAvatarAnimationView.alpha = visible ? tabBar.alpha : 0
        profileTabAvatarAnimationView.isHidden = !visible
        if profileTabAvatarAnimationFrames.count > 1 {
            startProfileTabAvatarAnimationTimerIfNeeded()
        } else {
            stopProfileTabAvatarAnimationTimer()
        }
    }

    private func startProfileTabAvatarAnimationTimerIfNeeded() {
        guard profileTabAvatarAnimationTimer == nil, profileTabAvatarAnimationFrames.count > 1 else { return }
        let timer = Timer(timeInterval: profileTabAvatarAnimationFrameDelay, repeats: true) { [weak self] _ in
            self?.advanceProfileTabAvatarAnimationFrame()
        }
        profileTabAvatarAnimationTimer = timer
        RunLoop.main.add(timer, forMode: .common)
    }

    private func stopProfileTabAvatarAnimationTimer() {
        profileTabAvatarAnimationTimer?.invalidate()
        profileTabAvatarAnimationTimer = nil
    }

    private func advanceProfileTabAvatarAnimationFrame() {
        guard profileTabAvatarAnimationFrames.count > 1 else {
            stopProfileTabAvatarAnimationTimer()
            return
        }
        profileTabAvatarAnimationFrameIndex =
            (profileTabAvatarAnimationFrameIndex + 1) % profileTabAvatarAnimationFrames.count
        let frame = profileTabAvatarAnimationFrames[profileTabAvatarAnimationFrameIndex]
        profileTabAvatarAnimationView.image = frame
        applyProfileTabAvatarFrame(frame)
    }

    private func applyProfileTabAvatarFrame(_ frame: UIImage) {
        tabBar.items?.forEach { item in
            guard NativeTab(tag: item.tag) == .settings else { return }
            item.image = frame
            item.selectedImage = frame
        }
        tabBar.setNeedsLayout()
        tabBar.layoutIfNeeded()
    }

    private func clearProfileTabAvatarAnimation() {
        profileTabStaticImageView?.alpha = 1
        profileTabStaticImageView = nil
        profileTabAvatarAnimationKey = nil
        stopProfileTabAvatarAnimationTimer()
        profileTabAvatarAnimationFrames = []
        profileTabAvatarAnimationFrameIndex = 0
        profileTabAvatarAnimationView.animationImages = nil
        profileTabAvatarAnimationView.image = nil
        profileTabAvatarAnimationView.frame = .zero
        profileTabAvatarAnimationView.alpha = 0
        profileTabAvatarAnimationView.isHidden = true
    }

    private func updateProfileTabAvatarAnimationFrame() {
        guard
            shouldShowNativeTabBar,
            profileTabAvatarAnimationView.image != nil,
            let imageFrame = profileTabNativeImageFrameInView()
        else {
            profileTabAvatarAnimationView.frame = .zero
            profileTabAvatarAnimationView.isHidden = true
            return
        }

        let side = min(max(max(imageFrame.width, imageFrame.height), 24), 34)
        profileTabAvatarAnimationView.frame = CGRect(
            x: imageFrame.midX - side / 2,
            y: imageFrame.midY - side / 2,
            width: side,
            height: side
        ).integral
        profileTabAvatarAnimationView.layer.cornerRadius = side / 2
        profileTabAvatarAnimationView.layer.masksToBounds = false
        profileTabAvatarAnimationView.isHidden = false
    }

    private func profileTabNativeImageFrameInView() -> CGRect? {
        guard let profileTabButton = nativeTabButton(for: .settings) else {
            profileTabStaticImageView?.alpha = 1
            profileTabStaticImageView = nil
            return nil
        }
        let fallbackFrame = profileTabFallbackAvatarFrame(in: profileTabButton)

        let imageViews = profileTabButton.recursiveSubviews(of: UIImageView.self).filter { imageView in
            imageView.bounds.width >= 12 &&
                imageView.bounds.height >= 12 &&
                imageView.bounds.width <= 44 &&
                imageView.bounds.height <= 44 &&
                !imageView.isHidden
        }

        guard let imageView = imageViews.min(by: {
            nativeProfileImageScore($0, in: profileTabButton) < nativeProfileImageScore($1, in: profileTabButton)
        }) else {
            profileTabStaticImageView?.alpha = 1
            profileTabStaticImageView = nil
            return fallbackFrame
        }

        if profileTabStaticImageView !== imageView {
            profileTabStaticImageView?.alpha = 1
            profileTabStaticImageView = imageView
        }
        imageView.alpha = 1
        let imageFrame = imageView.convert(imageView.bounds, to: view)
        return imageFrame.isEmpty ? fallbackFrame : imageFrame
    }

    private func profileTabFallbackAvatarFrame(in tabButton: UIView) -> CGRect {
        let buttonFrame = tabButton.convert(tabButton.bounds, to: view)
        let side: CGFloat = 28
        let center = CGPoint(
            x: buttonFrame.midX,
            y: currentNativeSelectedTab == .settings ? buttonFrame.midY - 14 : buttonFrame.minY + 24
        )
        return CGRect(
            x: center.x - side / 2,
            y: center.y - side / 2,
            width: side,
            height: side
        )
    }

    private func nativeProfileImageScore(_ imageView: UIImageView, in tabButton: UIView) -> CGFloat {
        let frame = imageView.convert(imageView.bounds, to: tabButton)
        let horizontalDistance = abs(frame.midX - tabButton.bounds.midX)
        let sizeDistance = abs(max(frame.width, frame.height) - 28)
        let lowerHalfPenalty: CGFloat = frame.midY > tabButton.bounds.midY ? 20 : 0
        return horizontalDistance + sizeDistance + lowerHalfPenalty
    }

    private func nativeTabButton(for tab: NativeTab) -> UIControl? {
        let tabs = visibleNativeTabs
        guard
            let tabIndex = tabs.firstIndex(of: tab),
            tabBar.bounds.width > 0
        else {
            return nil
        }

        let tabCount = CGFloat(tabs.count)
        let itemWidth = tabBar.bounds.width / tabCount
        let visualIndex: CGFloat
        if tabBar.effectiveUserInterfaceLayoutDirection == .rightToLeft {
            visualIndex = tabCount - 1 - CGFloat(tabIndex)
        } else {
            visualIndex = CGFloat(tabIndex)
        }
        let expectedCenterX = itemWidth * (visualIndex + 0.5)

        return tabBar.subviews.compactMap { $0 as? UIControl }
            .filter { !$0.isHidden && $0.bounds.width > 0 && $0.bounds.height > 0 }
            .min {
                abs($0.convert($0.bounds, to: tabBar).midX - expectedCenterX) <
                    abs($1.convert($1.bounds, to: tabBar).midX - expectedCenterX)
            }
    }

    private func refreshProfileAvatarImageIfNeeded() {
        let urlString = UserDefaults.standard.string(forKey: Self.nativeProfileAvatarURLKey)
        guard urlString != profileAvatarImageURL else { return }

        profileAvatarImageTask?.cancel()
        profileAvatarImageTask = nil
        profileAvatarImageURL = urlString
        profileAvatarImage = nil
        profileAvatarAnimationSourceFrames = []
        profileAvatarAnimationSourceDuration = 0
        clearProfileTabAvatarAnimation()

        guard let urlString, let url = URL(string: urlString) else { return }

        profileAvatarImageTask = URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard
                let self,
                let data,
                let payload = Self.profileAvatarImage(from: data)
            else { return }

            DispatchQueue.main.async {
                guard self.profileAvatarImageURL == urlString else { return }
                self.profileAvatarImage = payload.image
                self.profileAvatarAnimationSourceFrames = payload.animationFrames
                self.profileAvatarAnimationSourceDuration = payload.animationDuration
                self.applyNativeTabBarAppearance()
            }
        }
        profileAvatarImageTask?.resume()
    }

    private static func profileAvatarImage(from data: Data) -> ProfileAvatarImagePayload? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else {
            return UIImage(data: data).map {
                ProfileAvatarImagePayload(image: $0, animationFrames: [], animationDuration: 0)
            }
        }

        let frameCount = CGImageSourceGetCount(source)
        guard frameCount > 0 else {
            return UIImage(data: data).map {
                ProfileAvatarImagePayload(image: $0, animationFrames: [], animationDuration: 0)
            }
        }

        let thumbnailOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceThumbnailMaxPixelSize: 96,
        ]

        if frameCount > 1 {
            var frames: [UIImage] = []
            var duration: TimeInterval = 0

            for index in 0..<frameCount {
                guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, index, thumbnailOptions as CFDictionary) ??
                    CGImageSourceCreateImageAtIndex(source, index, nil)
                else {
                    continue
                }
                frames.append(UIImage(cgImage: cgImage, scale: UIScreen.main.scale, orientation: .up))
                duration += gifFrameDelay(source: source, index: index)
            }

            if frames.count > 1 {
                return ProfileAvatarImagePayload(
                    image: frames[0],
                    animationFrames: frames,
                    animationDuration: max(duration, Double(frames.count) * 0.02)
                )
            }
        }

        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, thumbnailOptions as CFDictionary) ??
            CGImageSourceCreateImageAtIndex(source, 0, nil)
        else {
            return UIImage(data: data).map {
                ProfileAvatarImagePayload(image: $0, animationFrames: [], animationDuration: 0)
            }
        }
        return ProfileAvatarImagePayload(
            image: UIImage(cgImage: cgImage, scale: UIScreen.main.scale, orientation: .up),
            animationFrames: [],
            animationDuration: 0
        )
    }

    private static func gifFrameDelay(source: CGImageSource, index: Int) -> TimeInterval {
        guard
            let properties = CGImageSourceCopyPropertiesAtIndex(source, index, nil) as? [CFString: Any],
            let gifProperties = properties[kCGImagePropertyGIFDictionary] as? [CFString: Any]
        else {
            return 0.1
        }

        let unclampedDelay = (gifProperties[kCGImagePropertyGIFUnclampedDelayTime] as? NSNumber)?.doubleValue
        let clampedDelay = (gifProperties[kCGImagePropertyGIFDelayTime] as? NSNumber)?.doubleValue
        let delay = unclampedDelay ?? clampedDelay ?? 0.1
        return delay < 0.02 ? 0.1 : delay
    }
}

private extension UIColor {
    convenience init?(hexString: String?) {
        guard var value = hexString?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        if value.hasPrefix("#") {
            value.removeFirst()
        }
        guard value.count == 6, let rgb = UInt64(value, radix: 16) else {
            return nil
        }
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}

private extension String {
    var nonEmpty: String? {
        isEmpty ? nil : self
    }
}

private extension UIView {
    func recursiveSubviews<T: UIView>(of type: T.Type) -> [T] {
        subviews.flatMap { subview -> [T] in
            var matches = (subview as? T).map { [$0] } ?? []
            matches.append(contentsOf: subview.recursiveSubviews(of: type))
            return matches
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Register MPV player bridge before Compose initializes
        NuvioPlayerRegistration.register()
        
        let controller = MainViewControllerKt.MainViewController()
        controller.view.backgroundColor = UIColor(red: 0.008, green: 0.016, blue: 0.016, alpha: 1.0)
        return RootComposeViewController(contentController: controller)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
