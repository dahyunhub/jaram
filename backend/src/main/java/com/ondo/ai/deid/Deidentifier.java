package com.ondo.ai.deid;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 비식별화 정책 컴포넌트(스테이트리스). 아동 실명을 요청 스코프 센티넬 [[CHILD_n]]로 치환한다.
 * 분석 경로에서만 사용하며 메모 저장 경로에는 개입하지 않는다(SM-1 입력속도 가드).
 * 인프라(OpenAiClient)를 직접 참조하지 않는다 — 의존성 방향 고정. 정본: docs/specs/ai-integration-spec.md §5.
 */
@Component
public class Deidentifier {

    /**
     * 반 전체 명단으로 요청 스코프 매핑을 만든다(MVP: 대상 아이뿐 아니라 본문에 등장할 수 있는 타 아동명도 보강 치환).
     * 토큰 번호는 명단 제공 순서대로 1..n(결정적). 빈/공백/중복 실명은 무시(중복 실명은 같은 토큰 재사용).
     */
    public RestorationContext newContext(Collection<String> rosterNames) {
        Map<String, String> nameToTokenByOrder = new LinkedHashMap<>();
        Map<String, String> tokenToName = new LinkedHashMap<>();
        int n = 1;
        for (String name : rosterNames) {
            if (name == null || name.isBlank() || nameToTokenByOrder.containsKey(name)) {
                continue;
            }
            String token = RestorationContext.token(n);
            nameToTokenByOrder.put(name, token);
            tokenToName.put(token, name);
            n++;
        }
        // 치환은 부분문자열 오치환 방지를 위해 '긴 이름부터'(예: "김민준"을 "김민"보다 먼저).
        Map<String, String> nameToTokenLongestFirst = new LinkedHashMap<>();
        nameToTokenByOrder.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
                .forEach(e -> nameToTokenLongestFirst.put(e.getKey(), e.getValue()));
        return new RestorationContext(nameToTokenLongestFirst, tokenToName);
    }

    /** 텍스트의 아동 실명을 센티넬 토큰으로 치환한다(긴 이름 우선). 분석 입력의 모든 텍스트 필드에 적용 가능. */
    public String deidentify(String text, RestorationContext context) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (Map.Entry<String, String> entry : context.nameToToken().entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
