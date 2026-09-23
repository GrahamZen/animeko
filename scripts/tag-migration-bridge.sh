#!/usr/bin/env bash
# 改分发包名的迁移: 给跳板包打 tag, 以及发布前的核对.
#
#   ./scripts/tag-migration-bridge.sh prepare 6.0.7 [基于的提交, 默认 HEAD] [提交时间, 默认 2030-01-01]
#   ./scripts/tag-migration-bridge.sh verify      # releases 列表最上面是谁、jsDelivr 解析到哪
#
# 跳板包是仍用旧 applicationId (me.him188.ani.*) 的一版, 文件名 ani-…. 已发布的老包只认这个前缀,
# 于是能像平常一样升级到它; 它再引导用户装落地版 (gradle.properties 的 ani.migration.landing.version),
# 落地版接管旧包的数据后自己更新到本项目的最新版 (见 AniBuildConfig.isMigrationBridge / isMigrationLanding).
# 跳板包与落地版都从迁移分支构建, 发布在 ani.migration.repository 里.
# CI 按 tag 的主版本号决定出哪套包: >= 6 出跳板包, 1.x ~ 5.x 出落地版 (见 fork-release.yml 的 line 步骤).
#
# prepare 只在本地做三件事, 不推送:
#
# 1. 在基于的提交之上造一个提交, 把 ci-helper/release-template.md 换成 release-template-bridge.md.
#    老包走镜像回落时读的是 tag 上的那份模板 (jsDelivr @latest), 只在 CI 里换的话它们看到的是新包的说明.
# 2. 这个提交的时间设在将来. GitHub 的 releases 列表按 **tag 所指提交的时间** 倒序排, 不看草稿哪个先建、
#    哪个先发布 (2026-09-22 用 API 核过: created_at 与提交时间逐秒一致). 而已发布的老包只看列表最上面那一个:
#    跳板包被新包下一次发版压下去之后, 还没升级的老用户看到的是 1.x, 判定"没有更新", 从此停在老版.
#    时间设在将来, 跳板包就一直排在最上面.
# 3. 打轻量 tag v<版本号>.
#
# 推送 tag 后 CI 出草稿. 发布前跑 verify, 确认跳板包排在最上面 (草稿对仓库主人可见, 排序与发布后一致).
# 发布顺序: 先发落地版, 再发跳板包 —— 反过来的话, 中间那段时间里升上跳板包的人找不到落地版.
#
# 新包的仓库在第一次打 tag 之前要做一次性清理 (新包从 1.x 起的前提):
#   - 上游的 v1.x ~ v5.x 有几百个 tag, 与新包的 tag 同名: 本地删掉, 并让 fetch upstream 不再带回来
#       git config remote.upstream.tagOpt --no-tags
#       git tag -l 'v[1-5].*' | xargs -r git tag -d
#   - 远端的 v5.x 会被新包在镜像上当成最新版 (新包按 <6 查 jsDelivr): 删掉
#       git ls-remote --tags origin 'v[1-5].*' | grep -v '\^{}' | sed 's#.*refs/tags/##' | xargs -r -n1 git push origin --delete
#
# 环境变量: REPO (放跳板包与落地版的仓库, 默认 GrahamZen/animeko)
set -euo pipefail

REPO="${REPO:-GrahamZen/animeko}"
BRIDGE_TEMPLATE="ci-helper/release-template-bridge.md"
RELEASE_TEMPLATE="ci-helper/release-template.md"

prepare() {
    local version="${1:?用法: $0 prepare <跳板版本号, 如 6.0.7> [基于的提交] [提交时间]}"
    local base="${2:-HEAD}"
    local date="${3:-2030-01-01T00:00:00+00:00}"

    local major="${version%%.*}"
    if ! [[ "$major" =~ ^[0-9]+$ ]] || [ "$major" -lt 6 ]; then
        echo "跳板包必须在旧版本线上 (主版本号 >= 6), 否则 CI 按新包构建: $version" >&2
        exit 1
    fi
    if [[ "$version" == *-* ]]; then
        echo "跳板包的版本号不能带 -: 带了会被标成预发布, 稳定频道的老用户看不到: $version" >&2
        exit 1
    fi
    local tag="v$version"
    if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
        echo "tag $tag 已经存在" >&2
        exit 1
    fi

    local base_commit bridge_blob tree commit
    base_commit="$(git rev-parse --verify "$base^{commit}")"
    bridge_blob="$(git rev-parse --verify "$base_commit:$BRIDGE_TEMPLATE")"

    # 用临时 index 造树, 不碰工作区和当前分支
    tmp="$(mktemp -d)"
    # 不能是 local: EXIT 时函数早已返回, 配上 set -u 就是 unbound variable
    trap 'rm -rf "${tmp:-}"' EXIT
    GIT_INDEX_FILE="$tmp/index" git read-tree "$base_commit"
    GIT_INDEX_FILE="$tmp/index" git update-index --cacheinfo "100644,$bridge_blob,$RELEASE_TEMPLATE"
    tree="$(GIT_INDEX_FILE="$tmp/index" git write-tree)"
    commit="$(GIT_AUTHOR_DATE="$date" GIT_COMMITTER_DATE="$date" \
        git commit-tree "$tree" -p "$base_commit" -m "release: 迁移跳板包 $tag")"
    git tag "$tag" "$commit"

    echo "已打 tag $tag -> $(git rev-parse --short "$commit") (基于 $(git rev-parse --short "$base_commit"), 提交时间 $date)"
    echo
    echo "接下来:"
    echo "  git push origin $tag      # 触发 CI, 出跳板包的草稿"
    echo "  $0 verify   # CI 建好草稿后, 确认它排在 releases 列表最上面"
}

verify() {
    echo "== releases 列表最上面三个 (含草稿; 已发布的老包只看第一个非草稿) =="
    gh api "repos/$REPO/releases?per_page=3" \
        --jq '.[] | "\(.tag_name)\tdraft=\(.draft)\tprerelease=\(.prerelease)\tcreated_at=\(.created_at)"'
    echo
    echo "== jsDelivr 解析到哪一版 (只含已推送的正式版 tag, 有最长 12 小时缓存) =="
    local spec
    for spec in latest "%3C6"; do
        printf '  @%-8s -> ' "$(printf '%b' "${spec//%/\\x}")"
        curl -s -o /dev/null -D - --max-time 20 \
            "https://cdn.jsdelivr.net/gh/$REPO@$spec/$RELEASE_TEMPLATE" |
            tr -d '\r' | sed -n 's/^x-jsd-version: *//Ip' | head -1
    done
    echo "  (@latest 是老包问的, 应为跳板包)"
}

case "${1:-}" in
    prepare) shift; prepare "$@" ;;
    verify) verify ;;
    *)
        sed -n '2,5p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
