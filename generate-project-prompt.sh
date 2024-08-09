#!/bin/bash

# 트리 구조를 출력하는 함수
function print_tree() {
    tree "$1" -I 'bin|build|gradle' --prune -f
}

# 파일 내용을 출력하는 함수
function print_file_contents() {
    local dir="$1"

    if [[ -f "$dir/.gitwhitelist" ]]; then
        grep -v '^#' "$dir/.gitwhitelist" | grep -v '^$' | while IFS= read -r pattern; do
            find "$dir" -path "$dir/$pattern" | while IFS= read -r file; do
                echo "<${file}>"
                echo '```'$(basename "$file" | awk -F. '{print $NF}')'```'
                cat "$file"
                echo '```'
                echo
            done
        done
    else
        # 화이트리스트 파일이 없으면 모든 파일 출력
        find "$dir" -type f | while IFS= read -r file; do
            echo "<${file}>"
            echo '```'$(basename "$file" | awk -F. '{print $NF}')'```'
            cat "$file"
            echo '```'
            echo
        done
    fi
}

# 스크립트 실행 부분
target_dir=${1:-.}

# 1. 하위 디렉토리와 파일을 포함한 트리구조 출력
print_tree "$target_dir"

# 2. 파일 내용을 루프 돌면서 출력
print_file_contents "$target_dir"