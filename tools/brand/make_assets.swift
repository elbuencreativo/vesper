// Genera el banner de Android TV y los PNG de marca de Vesper (sol poniente con rayas, paleta del launcher).
// Uso:  swift tools/brand/make_assets.swift <carpeta-del-proyecto>
// Salida: app/src/main/res/drawable-xhdpi/banner.png (320x180), drawable-xxhdpi/banner.png (640x360)
//         y branding/vesper-icon-512.png, branding/vesper-banner.png
import AppKit

let root = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "."

func c(_ hex: UInt32, _ a: CGFloat = 1) -> NSColor {
    NSColor(red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: a)
}

let navy = c(0x04071D)
let sunTop = c(0xF5BD42), sunMid = c(0xD7662A), sunBottom = c(0x901C16)

/// Dibuja el sol con rayas. (cx, cy) es el centro en coordenadas con origen abajo-izquierda; r el radio.
func drawSun(_ ctx: CGContext, cx: CGFloat, cy: CGFloat, r: CGFloat, background: NSColor) {
    ctx.saveGState()
    ctx.addEllipse(in: CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r))
    ctx.clip()
    let space = CGColorSpaceCreateDeviceRGB()
    let colors = [sunTop.cgColor, sunMid.cgColor, sunBottom.cgColor] as CFArray
    let gradient = CGGradient(colorsSpace: space, colors: colors, locations: [0, 0.5, 1])!
    ctx.drawLinearGradient(gradient, start: CGPoint(x: cx, y: cy + r), end: CGPoint(x: cx, y: cy - r), options: [])
    // Rayas del color del fondo en la mitad inferior; más gruesas hacia abajo.
    ctx.setFillColor(background.cgColor)
    let stripes: [(CGFloat, CGFloat)] = [(0.12, 0.045), (0.34, 0.075), (0.56, 0.11), (0.78, 0.15)]
    for (offset, thickness) in stripes {
        let y = cy - r * offset - r * thickness / 2
        ctx.fill(CGRect(x: cx - r, y: y, width: 2 * r, height: r * thickness))
    }
    ctx.restoreGState()
}

func render(width: Int, height: Int, draw: (CGContext) -> Void) -> NSBitmapImageRep {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8,
                               samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                               bytesPerRow: 0, bitsPerPixel: 0)!
    let gc = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = gc
    gc.cgContext.setShouldAntialias(true)
    draw(gc.cgContext)
    NSGraphicsContext.restoreGraphicsState()
    return rep
}

func save(_ rep: NSBitmapImageRep, _ path: String) {
    let url = URL(fileURLWithPath: root).appendingPathComponent(path)
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    try! rep.representation(using: .png, properties: [:])!.write(to: url)
    print("escrito \(path) (\(rep.pixelsWide)x\(rep.pixelsHigh))")
}

func banner(scale: CGFloat) -> NSBitmapImageRep {
    let w = Int(320 * scale), h = Int(180 * scale)
    return render(width: w, height: h) { ctx in
        ctx.setFillColor(navy.cgColor)
        ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))
        // Resplandor tenue de vino bajo el sol
        let space = CGColorSpaceCreateDeviceRGB()
        let glow = CGGradient(colorsSpace: space, colors: [c(0x320A17, 0.9).cgColor, c(0x04071D, 0).cgColor] as CFArray, locations: [0, 1])!
        ctx.drawRadialGradient(glow, startCenter: CGPoint(x: 78 * scale, y: 92 * scale), startRadius: 0,
                               endCenter: CGPoint(x: 78 * scale, y: 92 * scale), endRadius: 150 * scale, options: [])
        drawSun(ctx, cx: 78 * scale, cy: 92 * scale, r: 46 * scale, background: c(0x0B0A1E))
        // Nombre
        let paragraph = NSMutableParagraphStyle()
        paragraph.alignment = .left
        let attrs: [NSAttributedString.Key: Any] = [
            .font: NSFont(name: "AvenirNext-Bold", size: 34 * scale) ?? NSFont.boldSystemFont(ofSize: 34 * scale),
            .foregroundColor: c(0xFFF3E4),
            .kern: 6 * scale,
            .paragraphStyle: paragraph,
        ]
        NSAttributedString(string: "VESPER", attributes: attrs).draw(at: NSPoint(x: 142 * scale, y: 82 * scale))
        let small: [NSAttributedString.Key: Any] = [
            .font: NSFont(name: "AvenirNext-Medium", size: 9.5 * scale) ?? NSFont.systemFont(ofSize: 9.5 * scale),
            .foregroundColor: c(0xF5BD42, 0.9),
            .kern: 3 * scale,
        ]
        NSAttributedString(string: "TV LAUNCHER", attributes: small).draw(at: NSPoint(x: 144 * scale, y: 64 * scale))
    }
}

func icon(size: Int) -> NSBitmapImageRep {
    let s = CGFloat(size)
    return render(width: size, height: size) { ctx in
        let path = CGPath(roundedRect: CGRect(x: 0, y: 0, width: s, height: s), cornerWidth: s * 0.225, cornerHeight: s * 0.225, transform: nil)
        ctx.addPath(path)
        ctx.clip()
        ctx.setFillColor(navy.cgColor)
        ctx.fill(CGRect(x: 0, y: 0, width: s, height: s))
        drawSun(ctx, cx: s / 2, cy: s * 0.52, r: s * 0.30, background: navy)
    }
}

save(banner(scale: 1), "app/src/main/res/drawable-xhdpi/banner.png")
save(banner(scale: 2), "app/src/main/res/drawable-xxhdpi/banner.png")
save(banner(scale: 2), "branding/vesper-banner.png")
save(icon(size: 512), "branding/vesper-icon-512.png")
