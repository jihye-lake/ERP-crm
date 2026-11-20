/*
 * 화면 금액 표시: 천 단위 콤마 + 한글 금액(일억구천이백삼십만원).
 * 서버 쪽 같은 규칙은 com.nexora.erp.web.MoneyFormat (여기를 고치면 거기도 같이 맞출 것!)
 */
window.Money = (function () {
    const DIGITS = ["", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"];
    const SMALL_UNITS = ["", "십", "백", "천"];
    const BIG_UNITS = ["", "만", "억", "조", "경", "해"];

    function toNumberString(v) {
        // "￦ 1,234,000" / 1234000 / "1234000.00" → "1234000.00", 숫자가 아니면 null
        const s = String(v === null || v === undefined ? "" : v).replace(/[^0-9.\-]/g, "");
        return /^-?\d+(\.\d+)?$/.test(s) ? s : null;
    }

    function comma(v) {
        const s = toNumberString(v);
        if (s === null) return "0";
        const neg = s.startsWith("-");
        const [intPart, fracPart] = s.replace("-", "").split(".");
        const grouped = intPart.replace(/^0+(?=\d)/, "").replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        const frac = (fracPart || "").replace(/0+$/, "").substring(0, 2);
        return (neg ? "-" : "") + grouped + (frac ? "." + frac : "");
    }

    function groupToKorean(group) {
        let out = "";
        for (let pos = 3; pos >= 0; pos--) {
            const digit = Math.floor(group / Math.pow(10, pos)) % 10;
            if (digit === 0) continue;
            out += (digit === 1 && pos > 0 ? "" : DIGITS[digit]) + SMALL_UNITS[pos];
        }
        return out;
    }

    function korean(v) {
        const s = toNumberString(v);
        if (s === null) return "";
        const neg = s.startsWith("-");
        let intPart = s.replace("-", "").split(".")[0].replace(/^0+/, "");
        if (intPart === "") return "영원";
        // 큰 금액도 정확하게: 뒤에서부터 4자리씩 끊어서 처리 (Number 정밀도 문제 회피)
        let result = "";
        let unitIndex = 0;
        while (intPart.length > 0 && unitIndex < BIG_UNITS.length) {
            const group = parseInt(intPart.slice(-4), 10);
            intPart = intPart.slice(0, -4);
            if (group > 0) {
                const text = (unitIndex === 1 && group === 1) ? "" : groupToKorean(group);
                result = text + BIG_UNITS[unitIndex] + result;
            }
            unitIndex++;
        }
        return (neg ? "마이너스 " : "") + result + "원";
    }

    return {comma: comma, korean: korean};
})();
