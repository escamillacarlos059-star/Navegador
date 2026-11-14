#include "vectra_engine.hpp"

#include <algorithm>
#include <cctype>
#include <chrono>
#include <iomanip>
#include <mutex>
#include <sstream>

namespace vectra {

namespace {

constexpr const char* kTrackingKeys[] = {
    "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
    "utm_id", "fbclid", "gclid", "dclid", "msclkid", "yclid", "mc_cid",
    "mc_eid", "igshid", "igsh", "_hsenc", "_hsmi", "mkt_tok", "ref_src",
    "si", "spm"
};

constexpr const char* kBuiltinBlockedDomains[] = {
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "google-analytics.com",
    "googletagmanager.com",
    "googletagservices.com",
    "pagead2.googlesyndication.com",
    "adservice.google.com",
    "connect.facebook.net",
    "an.facebook.com",
    "pixel.facebook.com",
    "ads-twitter.com",
    "analytics.twitter.com",
    "scorecardresearch.com",
    "quantserve.com",
    "adnxs.com",
    "criteo.com",
    "criteo.net",
    "taboola.com",
    "outbrain.com",
    "hotjar.com",
    "mixpanel.com",
    "segment.io",
    "segment.com",
    "amplitude.com",
    "pubmatic.com",
    "rubiconproject.com",
    "openx.net",
    "amazon-adsystem.com",
    "moatads.com",
    "chartbeat.com",
    "newrelic.com",
    "nr-data.net",
    "omtrdc.net",
    "demdex.net",
    "casalemedia.com",
    "advertising.com",
    "adsrvr.org",
    "adroll.com",
    "bidswitch.net",
    "branch.io",
    "app.link",
    "appsflyer.com",
    "adjust.com",
    "kochava.com",
    "sentry.io",
    "bugsnag.com",
    "clarity.ms",
    "bat.bing.com",
    "mc.yandex.ru",
    "yandexadexchange.net",
    "mgid.com",
    "revcontent.com",
    "zedo.com",
    "mathtag.com",
    "bluekai.com",
    "krxd.net",
    "rlcdn.com",
    "tapad.com",
    "yieldmo.com",
    "sharethrough.com",
    "triplelift.com",
    "smartadserver.com",
    "mediavine.com",
    "adcolony.com",
    "unityads.unity3d.com",
    "vungle.com",
    "applovin.com"
};

std::string build_search_url(std::string_view encoded_query, int engine_id) {
    switch (engine_id) {
        case 1: // DuckDuckGo
            return "https://duckduckgo.com/?q=" + std::string(encoded_query);
        case 2: // Brave Search
            return "https://search.brave.com/search?q=" + std::string(encoded_query);
        case 3: // Wikipedia (ES)
            return "https://es.wikipedia.org/w/index.php?search=" + std::string(encoded_query);
        case 0: // Google
        default:
            return "https://www.google.com/search?q=" + std::string(encoded_query);
    }
}

std::string get_abi_name() {
#if defined(__aarch64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "unknown-abi";
#endif
}

std::string get_compiler_string() {
#if defined(__clang__)
    std::ostringstream oss;
    oss << "Clang " << __clang_major__ << "." << __clang_minor__ << "." << __clang_patchlevel__;
    return oss.str();
#elif defined(__GNUC__)
    std::ostringstream oss;
    oss << "GCC " << __GNUC__ << "." << __GNUC_MINOR__;
    return oss.str();
#else
    return "C++ Compiler";
#endif
}

} // namespace

VectraEngine& VectraEngine::instance() {
    static VectraEngine engine;
    return engine;
}

VectraEngine::VectraEngine() {
    for (const char* domain : kBuiltinBlockedDomains) {
        builtin_blocked_domains_.emplace(domain);
    }
    for (const char* key : kTrackingKeys) {
        tracking_query_keys_.emplace(key);
    }
}

std::string VectraEngine::to_lower_ascii(std::string_view input) {
    std::string out;
    out.reserve(input.size());
    for (unsigned char ch : input) {
        out.push_back(static_cast<char>(std::tolower(ch)));
    }
    return out;
}

std::string VectraEngine::trim_ascii(std::string_view input) {
    std::size_t start = 0;
    while (start < input.size() && std::isspace(static_cast<unsigned char>(input[start]))) {
        ++start;
    }
    std::size_t end = input.size();
    while (end > start && std::isspace(static_cast<unsigned char>(input[end - 1]))) {
        --end;
    }
    return std::string(input.substr(start, end - start));
}

std::string VectraEngine::url_encode(std::string_view value) {
    std::ostringstream escaped;
    escaped.fill('0');
    escaped << std::hex << std::uppercase;

    for (unsigned char c : value) {
        if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
            escaped << static_cast<char>(c);
        } else if (c == ' ') {
            escaped << '+';
        } else {
            escaped << '%' << std::setw(2) << static_cast<int>(c);
        }
    }
    return escaped.str();
}

bool VectraEngine::is_ipv4_literal(std::string_view host) {
    if (host.empty()) return false;
    int dots = 0;
    int part_val = 0;
    int part_len = 0;
    for (char ch : host) {
        if (ch == '.') {
            if (part_len == 0 || part_val > 255) return false;
            ++dots;
            part_val = 0;
            part_len = 0;
        } else if (ch >= '0' && ch <= '9') {
            part_val = part_val * 10 + (ch - '0');
            ++part_len;
            if (part_len > 3 || part_val > 255) return false;
        } else {
            return false;
        }
    }
    return dots == 3 && part_len > 0 && part_val <= 255;
}

bool VectraEngine::looks_like_host_or_url(std::string_view trimmed) {
    if (trimmed.empty()) return false;
    for (unsigned char ch : trimmed) {
        if (std::isspace(ch)) return false;
    }

    const std::string lower = to_lower_ascii(trimmed);
    if (lower.starts_with("http://") || lower.starts_with("https://") ||
        lower.starts_with("vectra://") || lower.starts_with("about:") ||
        lower.starts_with("file://")) {
        return true;
    }

    if (lower.starts_with("localhost") &&
        (lower.size() == 9 || lower[9] == ':' || lower[9] == '/')) {
        return true;
    }

    const auto slash_pos = trimmed.find('/');
    const std::string_view host_part = (slash_pos == std::string_view::npos)
        ? trimmed
        : trimmed.substr(0, slash_pos);
    const auto colon_pos = host_part.find(':');
    const std::string_view pure_host = (colon_pos == std::string_view::npos)
        ? host_part
        : host_part.substr(0, colon_pos);

    if (is_ipv4_literal(pure_host)) {
        return true;
    }

    const auto dot_pos = pure_host.find('.');
    if (dot_pos == std::string_view::npos || dot_pos == 0 || dot_pos == pure_host.size() - 1) {
        return false;
    }

    const std::string_view tld = pure_host.substr(pure_host.rfind('.') + 1);
    if (tld.size() < 2 || tld.size() > 24) {
        return false;
    }
    for (unsigned char ch : tld) {
        if (!std::isalnum(ch)) return false;
    }
    return true;
}

std::string VectraEngine::extract_host(std::string_view url) const {
    std::string trimmed = trim_ascii(url);
    std::string_view view(trimmed);

    const auto scheme_pos = view.find("://");
    if (scheme_pos != std::string_view::npos) {
        view.remove_prefix(scheme_pos + 3);
    }

    const auto at_pos = view.find('@');
    const auto slash_pos = view.find('/');
    if (at_pos != std::string_view::npos && (slash_pos == std::string_view::npos || at_pos < slash_pos)) {
        view.remove_prefix(at_pos + 1);
    }

    const auto end_pos = view.find_first_of("/?#");
    if (end_pos != std::string_view::npos) {
        view = view.substr(0, end_pos);
    }

    if (!view.empty() && view.front() == '[') {
        const auto close_bracket = view.find(']');
        if (close_bracket != std::string_view::npos) {
            return to_lower_ascii(view.substr(0, close_bracket + 1));
        }
    }

    const auto colon_pos = view.find(':');
    if (colon_pos != std::string_view::npos) {
        view = view.substr(0, colon_pos);
    }

    return to_lower_ascii(view);
}

std::string VectraEngine::strip_tracking_query_params(std::string_view url, int& stripped_count) const {
    stripped_count = 0;
    const auto q_pos = url.find('?');
    if (q_pos == std::string_view::npos) {
        return std::string(url);
    }

    const auto hash_pos = url.find('#', q_pos);
    const std::string_view base = url.substr(0, q_pos);
    const std::string_view query = (hash_pos == std::string_view::npos)
        ? url.substr(q_pos + 1)
        : url.substr(q_pos + 1, hash_pos - q_pos - 1);
    const std::string_view fragment = (hash_pos == std::string_view::npos)
        ? std::string_view{}
        : url.substr(hash_pos);

    std::vector<std::string_view> kept_params;
    std::size_t start = 0;
    while (start <= query.size()) {
        const auto amp = query.find('&', start);
        const std::string_view pair = (amp == std::string_view::npos)
            ? query.substr(start)
            : query.substr(start, amp - start);

        if (!pair.empty()) {
            const auto eq = pair.find('=');
            const std::string_view raw_key = (eq == std::string_view::npos) ? pair : pair.substr(0, eq);
            const std::string lower_key = to_lower_ascii(raw_key);
            if (tracking_query_keys_.contains(lower_key)) {
                ++stripped_count;
            } else {
                kept_params.push_back(pair);
            }
        }

        if (amp == std::string_view::npos) break;
        start = amp + 1;
    }

    if (stripped_count == 0) {
        return std::string(url);
    }

    std::string out(base);
    if (!kept_params.empty()) {
        out.push_back('?');
        for (std::size_t i = 0; i < kept_params.size(); ++i) {
            if (i > 0) out.push_back('&');
            out.append(kept_params[i]);
        }
    }
    out.append(fragment);
    return out;
}

NormalizedUrlResult VectraEngine::normalize_input(
    std::string_view raw_input,
    int search_engine_id,
    bool strip_tracking_params
) {
    NormalizedUrlResult result;
    const std::string trimmed = trim_ascii(raw_input);
    if (trimmed.empty()) {
        result.final_url = "vectra://home";
        result.display_host = "vectra://home";
        return result;
    }

    const std::string lower = to_lower_ascii(trimmed);
    if (lower == "vectra://home") {
        result.final_url = "vectra://home";
        result.display_host = "vectra://home";
        return result;
    }

    if (looks_like_host_or_url(trimmed)) {
        std::string candidate;
        if (lower.starts_with("http://") || lower.starts_with("https://") ||
            lower.starts_with("file://") || lower.starts_with("about:")) {
            candidate = trimmed;
        } else if (lower.starts_with("localhost") || is_ipv4_literal(extract_host(trimmed))) {
            candidate = "http://" + trimmed;
        } else {
            candidate = "https://" + trimmed;
        }

        if (strip_tracking_params) {
            int stripped = 0;
            candidate = strip_tracking_query_params(candidate, stripped);
            result.stripped_params_count = stripped;
            if (stripped > 0) {
                total_params_stripped_.fetch_add(static_cast<std::uint64_t>(stripped), std::memory_order_relaxed);
            }
        }

        result.final_url = std::move(candidate);
        result.display_host = extract_host(result.final_url);
        result.is_search_query = false;
        return result;
    }

    result.is_search_query = true;
    result.final_url = build_search_url(url_encode(trimmed), search_engine_id);
    result.display_host = extract_host(result.final_url);
    return result;
}

bool VectraEngine::should_block_request(
    std::string_view request_url,
    std::string_view page_host
) {
    const auto t_start = std::chrono::steady_clock::now();
    total_requests_inspected_.fetch_add(1, std::memory_order_relaxed);

    const std::string host = extract_host(request_url);
    if (host.empty()) {
        return false;
    }

    const std::string lower_page_host = to_lower_ascii(page_host);
    // Allow first-party exact host requests so core pages are never broken
    if (!lower_page_host.empty() && host == lower_page_host) {
        const auto t_end = std::chrono::steady_clock::now();
        last_lookup_nanos_.store(
            static_cast<std::uint64_t>(std::chrono::duration_cast<std::chrono::nanoseconds>(t_end - t_start).count()),
            std::memory_order_relaxed
        );
        return false;
    }

    bool blocked = false;
    {
        std::shared_lock lock(rules_mutex_);
        std::string_view candidate(host);
        while (!candidate.empty()) {
            if (builtin_blocked_domains_.contains(candidate) || custom_blocked_domains_.contains(candidate)) {
                blocked = true;
                break;
            }
            const auto dot = candidate.find('.');
            if (dot == std::string_view::npos) break;
            candidate.remove_prefix(dot + 1);
        }
    }

    if (blocked) {
        total_trackers_blocked_.fetch_add(1, std::memory_order_relaxed);
    }

    const auto t_end = std::chrono::steady_clock::now();
    last_lookup_nanos_.store(
        static_cast<std::uint64_t>(std::chrono::duration_cast<std::chrono::nanoseconds>(t_end - t_start).count()),
        std::memory_order_relaxed
    );
    return blocked;
}

SecurityLevel VectraEngine::analyze_security(std::string_view url) const {
    const std::string lower = to_lower_ascii(trim_ascii(url));
    if (lower.empty() || lower.starts_with("vectra://") || lower.starts_with("about:")) {
        return SecurityLevel::Internal;
    }

    const std::string host = extract_host(lower);
    if (host.find("xn--") != std::string::npos) {
        return SecurityLevel::SuspiciousPunycode;
    }
    if (is_ipv4_literal(host)) {
        return SecurityLevel::IpLiteral;
    }
    if (lower.starts_with("https://")) {
        return SecurityLevel::SecureHttps;
    }
    return SecurityLevel::InsecureHttp;
}

bool VectraEngine::add_custom_blocked_domain(std::string_view domain) {
    const std::string host = extract_host(domain);
    if (host.empty() || host.find('.') == std::string::npos) {
        return false;
    }
    std::unique_lock lock(rules_mutex_);
    return custom_blocked_domains_.insert(host).second;
}

bool VectraEngine::remove_custom_blocked_domain(std::string_view domain) {
    const std::string host = extract_host(domain);
    std::unique_lock lock(rules_mutex_);
    return custom_blocked_domains_.erase(host) > 0;
}

std::vector<std::string> VectraEngine::get_custom_blocked_domains() const {
    std::shared_lock lock(rules_mutex_);
    std::vector<std::string> domains(custom_blocked_domains_.begin(), custom_blocked_domains_.end());
    std::sort(domains.begin(), domains.end());
    return domains;
}

EngineDiagnostics VectraEngine::get_diagnostics() const {
    EngineDiagnostics diag;
#if __cplusplus >= 202002L
    diag.cpp_standard = "C++20 (ISO/IEC 14882:2020)";
#else
    diag.cpp_standard = "C++ (" + std::to_string(__cplusplus) + ")";
#endif
    diag.compiler_info = get_compiler_string();
    diag.target_abi = get_abi_name();
    {
        std::shared_lock lock(rules_mutex_);
        diag.filter_rules_count = builtin_blocked_domains_.size();
        diag.custom_rules_count = custom_blocked_domains_.size();
    }
    diag.total_requests_inspected = total_requests_inspected_.load(std::memory_order_relaxed);
    diag.total_trackers_blocked = total_trackers_blocked_.load(std::memory_order_relaxed);
    diag.total_params_stripped = total_params_stripped_.load(std::memory_order_relaxed);
    diag.last_lookup_nanos = last_lookup_nanos_.load(std::memory_order_relaxed);
    return diag;
}

void VectraEngine::reset_stats() {
    total_requests_inspected_.store(0, std::memory_order_relaxed);
    total_trackers_blocked_.store(0, std::memory_order_relaxed);
    total_params_stripped_.store(0, std::memory_order_relaxed);
    last_lookup_nanos_.store(0, std::memory_order_relaxed);
}

PeInspectionResult VectraEngine::inspect_pe_bytes(const std::uint8_t* data, std::size_t size) const {
    PeInspectionResult res;
    if (data == nullptr || size < 64) {
        res.status_detail = "Archivo demasiado pequeño o vacío para ser un ejecutable PE válido.";
        return res;
    }

    auto read_u16 = [data](std::size_t off) -> std::uint16_t {
        return static_cast<std::uint16_t>(data[off]) |
               (static_cast<std::uint16_t>(data[off + 1]) << 8);
    };
    auto read_u32 = [data](std::size_t off) -> std::uint32_t {
        return static_cast<std::uint32_t>(data[off]) |
               (static_cast<std::uint32_t>(data[off + 1]) << 8) |
               (static_cast<std::uint32_t>(data[off + 2]) << 16) |
               (static_cast<std::uint32_t>(data[off + 3]) << 24);
    };

    // Check DOS 'MZ' signature (0x5A4D)
    if (read_u16(0) != 0x5A4D) {
        res.status_detail = "Sin cabecera DOS 'MZ': no es un ejecutable Windows PE (.exe).";
        return res;
    }

    const std::uint32_t pe_offset = read_u32(0x3C);
    if (static_cast<std::size_t>(pe_offset) + 24 > size) {
        res.status_detail = "Desplazamiento de cabecera PE fuera de rango en los bytes leídos.";
        return res;
    }

    // Check 'PE\0\0' signature (0x00004550)
    if (read_u32(pe_offset) != 0x00004550) {
        res.status_detail = "Cabecera DOS válida, pero sin firma 'PE\\0\\0' de Windows.";
        return res;
    }

    const std::size_t coff_off = pe_offset + 4;
    const std::uint16_t machine = read_u16(coff_off);
    const std::uint16_t num_sections = read_u16(coff_off + 2);
    const std::uint16_t opt_header_size = read_u16(coff_off + 16);

    res.is_valid_pe = true;
    res.sections_count = static_cast<int>(num_sections);
    res.architecture = [machine]() -> std::string {
        switch (machine) {
            case 0x014c: return "x86 (32-bit i386)";
            case 0x8664: return "x64 (AMD64 64-bit)";
            case 0xAA64: return "ARM64 (AArch64)";
            case 0x01c4: return "ARMv7 Thumb-2";
            default: {
                std::ostringstream oss;
                oss << "Machine 0x" << std::hex << std::uppercase << machine;
                return oss.str();
            }
        }
    }();

    const std::size_t opt_off = coff_off + 20;
    if (opt_header_size >= 2 && opt_off + 2 <= size) {
        const std::uint16_t magic = read_u16(opt_off);
        res.pe_format = (magic == 0x020B) ? "PE32+ (64-bit)" : (magic == 0x010B ? "PE32 (32-bit)" : "Desconocido");

        // Subsystem field is at offset 68 in both PE32 and PE32+ optional headers
        if (opt_header_size >= 70 && opt_off + 70 <= size) {
            const std::uint16_t subsys = read_u16(opt_off + 68);
            res.subsystem = [subsys]() -> std::string {
                switch (subsys) {
                    case 1: return "Native Windows";
                    case 2: return "Windows GUI";
                    case 3: return "Windows Console (CUI)";
                    case 9: return "Windows CE";
                    case 10: return "EFI Application";
                    default: return "Subsistema #" + std::to_string(subsys);
                }
            }();
        } else {
            res.subsystem = "No especificado";
        }
    } else {
        res.pe_format = "COFF sin cabecera opcional";
        res.subsystem = "N/A";
    }

    res.status_detail = "Binario Windows PE válido analizado en C++20.";
    return res;
}

} // namespace vectra
