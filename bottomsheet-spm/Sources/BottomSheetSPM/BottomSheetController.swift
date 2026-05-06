import UIKit

// MARK: - Bottom Sheet VC

@objcMembers
public class BottomSheetController: UIViewController {
    private var contentViewController: UIViewController?
    private var screenshotImageView: UIImageView?
    private let transitioningDel = BottomSheetTransitioningDelegate()

    public var onDismissHandler: (() -> Void)?

    public init(contentViewController: UIViewController) {
        self.contentViewController = contentViewController
        super.init(nibName: nil, bundle: nil)
        transitioningDelegate = transitioningDel
        modalPresentationStyle = .custom
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override public func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        view.isOpaque = false

        guard let contentVC = contentViewController else { return }

        addChild(contentVC)
        contentVC.view.backgroundColor = .clear
        contentVC.view.isOpaque = false
        view.addSubview(contentVC.view)
        contentVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            contentVC.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentVC.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            contentVC.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentVC.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        contentVC.didMove(toParent: self)
    }

    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        if let contentView = contentViewController?.view {
            makeTransparent(contentView)
        }
    }

    private func makeTransparent(_ targetView: UIView) {
        targetView.backgroundColor = .clear
        targetView.isOpaque = false
        targetView.layer.backgroundColor = UIColor.clear.cgColor

        for subview in targetView.subviews {
            makeTransparent(subview)
        }
    }

    public func replaceContentWithScreenshot() {
        guard let contentVC = contentViewController else { return }

        let renderer = UIGraphicsImageRenderer(size: contentVC.view.bounds.size)
        let image = renderer.image { _ in
            contentVC.view.drawHierarchy(in: contentVC.view.bounds, afterScreenUpdates: true)
        }

        let finalImage: UIImage
        if image.size.width <= 0 || image.size.height <= 0 {
            let fallback = UIGraphicsImageRenderer(size: contentVC.view.bounds.size)
            finalImage = fallback.image { ctx in
                UIColor.systemBackground.setFill()
                ctx.fill(CGRect(origin: .zero, size: contentVC.view.bounds.size))
            }
        } else {
            finalImage = image
        }

        contentVC.view.isHidden = true

        let imageView = UIImageView(image: finalImage)
        imageView.frame = contentVC.view.frame
        imageView.contentMode = .scaleToFill
        view.addSubview(imageView)
        screenshotImageView = imageView
    }

    override public func dismiss(animated flag: Bool, completion: (() -> Void)? = nil) {
        if flag && view.window != nil {
            replaceContentWithScreenshot()
        }
        super.dismiss(animated: flag) {
            self.onDismissHandler?()
            self.screenshotImageView?.removeFromSuperview()
            self.screenshotImageView = nil
            completion?()
        }
    }
}

// MARK: - Presentation Controller

class BottomSheetPresentationController: UIPresentationController {
    private let dimmingView = UIView()

    override func presentationTransitionWillBegin() {
        guard let containerView = containerView else { return }

        dimmingView.frame = containerView.bounds
        dimmingView.backgroundColor = UIColor(white: 0, alpha: 0.3)
        dimmingView.alpha = 0
        containerView.insertSubview(dimmingView, at: 0)

        let tap = UITapGestureRecognizer(target: self, action: #selector(dimmingTapped))
        dimmingView.addGestureRecognizer(tap)

        presentedViewController.transitionCoordinator?.animate(alongsideTransition: { _ in
            self.dimmingView.alpha = 1
        })
    }

    override func dismissalTransitionWillBegin() {
        presentedViewController.transitionCoordinator?.animate(alongsideTransition: { _ in
            self.dimmingView.alpha = 0
        })
    }

    override func dismissalTransitionDidEnd(_ completed: Bool) {
        if completed { dimmingView.removeFromSuperview() }
    }

    override var frameOfPresentedViewInContainerView: CGRect {
        guard let containerView = containerView else { return .zero }
        return containerView.bounds
    }

    override func containerViewDidLayoutSubviews() {
        super.containerViewDidLayoutSubviews()
        dimmingView.frame = containerView?.bounds ?? .zero
        guard presentedViewController.transitionCoordinator == nil else { return }
        presentedView?.frame = frameOfPresentedViewInContainerView
    }

    @objc private func dimmingTapped() {
        presentedViewController.dismiss(animated: true)
    }
}

// MARK: - Transitioning Delegate

class BottomSheetTransitioningDelegate: NSObject, UIViewControllerTransitioningDelegate {
    func presentationController(forPresented presented: UIViewController,
                                presenting: UIViewController?,
                                source: UIViewController) -> UIPresentationController? {
        BottomSheetPresentationController(presentedViewController: presented, presenting: presenting)
    }

    func animationController(forPresented presented: UIViewController,
                             presenting: UIViewController,
                             source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        BottomSheetAnimator(isPresenting: true)
    }

    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        BottomSheetAnimator(isPresenting: false)
    }
}

// MARK: - Animator

class BottomSheetAnimator: NSObject, UIViewControllerAnimatedTransitioning {
    private let isPresenting: Bool

    init(isPresenting: Bool) {
        self.isPresenting = isPresenting
    }

    func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        0.15
    }

    func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        if isPresenting {
            animatePresent(using: transitionContext)
        } else {
            animateDismiss(using: transitionContext)
        }
    }

    private static func bottomAnchoredTransform(viewHeight: CGFloat, scale: CGFloat) -> CGAffineTransform {
        let offsetY = (1 - scale) * viewHeight / 2
        return CGAffineTransform(translationX: 0, y: offsetY).scaledBy(x: scale, y: scale)
    }

    private func animatePresent(using transitionContext: UIViewControllerContextTransitioning) {
        guard let toView = transitionContext.view(forKey: .to) else {
            transitionContext.completeTransition(false)
            return
        }
        let finalFrame = transitionContext.finalFrame(for: transitionContext.viewController(forKey: .to)!)

        toView.frame = finalFrame
        toView.alpha = 0
        toView.transform = Self.bottomAnchoredTransform(viewHeight: finalFrame.height, scale: 0.8)
        transitionContext.containerView.addSubview(toView)

        UIView.animate(withDuration: 0.15, delay: 0, options: .curveEaseOut) {
            toView.alpha = 1
            toView.transform = .identity
        } completion: { _ in
            transitionContext.completeTransition(!transitionContext.transitionWasCancelled)
        }
    }

    private func animateDismiss(using transitionContext: UIViewControllerContextTransitioning) {
        guard let fromView = transitionContext.view(forKey: .from) else {
            transitionContext.completeTransition(false)
            return
        }

        UIView.animate(withDuration: 0.15, delay: 0, options: .curveEaseIn) {
            fromView.alpha = 0
            fromView.transform = Self.bottomAnchoredTransform(viewHeight: fromView.frame.height, scale: 0.8)
        } completion: { _ in
            fromView.transform = .identity
            fromView.removeFromSuperview()
            transitionContext.completeTransition(!transitionContext.transitionWasCancelled)
        }
    }
}
