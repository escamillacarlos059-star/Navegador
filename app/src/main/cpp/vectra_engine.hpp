#pragma once

#include <atomic>
#include <cstdint>
#include <functional>
#include <shared_mutex>
#include <string>
#include <string_view>
#include <unordered_set>
#include <vector>

namespace vectra {

struct TransparentStringHash {
    using is_transparent = void;
    std::size_t operator()(std::string_view sv) const noexcept {
        return std::hash<std::string_view>{}(sv);
    }
    std::size_t operator()(const std::string& s) const noexcept {
        return std::hash<std::string_view>{}(s);
    }
    std::size_t operator()(const char* s) const noexcept {
        return std::hash<std::string_view>{}(s);
    }
};

enum class SecurityLevel : int {
    Internal = 0,
    SecureHttps = 1,
    InsecureHttp = 2,
    SuspiciousPunycode = 3,
    IpLiteral = 4
};

struct NormalizedUrlResult {
    std::string final_url;
    std::string display_host;
    bool is_search_query = false;
    int stripped_params_count = 0;
};

struct EngineDiagnostics {
    std::string cpp_standard;
    std::string compiler_info;
    std::string target_abi;
    std::size_t filter_rules_count = 0;
    std::size_t custom_rules_count = 0;
    std::uint64_t total_requests_inspected = 0;
    std::uint64_t total_trackers_blocked = 0;
    std::uint64_t total_params_stripped = 0;
    std::uint64_t last_lookup_nanos = 0;
};

struct PeInspectionResult {
    bool is_valid_pe = false;
    std::string architecture;
    std::string pe_format;
    std::string subsystem;
    int sections_count = 0;
    std::string status_detail;
};

class VectraEngine {
public:
    static VectraEngine& instance();

    NormalizedUrlResult normalize_input(
        std::string_view raw_input,
        int search_engine_id,
        bool strip_tracking_params
    );

    bool should_block_request(
        std::string_view request_url,
        std::string_view page_host
    );

    SecurityLevel analyze_security(std::string_view url) const;
    std::string extract_host(std::string_view url) const;
    std::string strip_tracking_query_params(std::string_view url, int& stripped_count) const;

    bool add_custom_blocked_domain(std::string_view domain);
    bool remove_custom_blocked_domain(std::string_view domain);
    std::vector<std::string> get_custom_blocked_domains() const;

    EngineDiagnostics get_diagnostics() const;
    PeInspectionResult inspect_pe_bytes(const std::uint8_t* data, std::size_t size) const;
    void reset_stats();

private:
    VectraEngine();

    static std::string to_lower_ascii(std::string_view input);
    static std::string trim_ascii(std::string_view input);
    static std::string url_encode(std::string_view value);
    static bool looks_like_host_or_url(std::string_view trimmed);
    static bool is_ipv4_literal(std::string_view host);

    mutable std::shared_mutex rules_mutex_;
    std::unordered_set<std::string, TransparentStringHash, std::equal_to<>> builtin_blocked_domains_;
    std::unordered_set<std::string, TransparentStringHash, std::equal_to<>> custom_blocked_domains_;
    std::unordered_set<std::string, TransparentStringHash, std::equal_to<>> tracking_query_keys_;

    std::atomic<std::uint64_t> total_requests_inspected_{0};
    std::atomic<std::uint64_t> total_trackers_blocked_{0};
    std::atomic<std::uint64_t> total_params_stripped_{0};
    std::atomic<std::uint64_t> last_lookup_nanos_{0};
};

} // namespace vectra
