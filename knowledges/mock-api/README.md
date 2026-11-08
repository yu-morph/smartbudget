# SmartBudget Mock API

대학교 과제용 가계부 앱을 위한 고정 거래내역 API입니다.

실제 금융기관과 연동하지 않습니다. 공개 데이터셋을 한국형 거래 JSON으로 변환한 뒤, FastAPI가 조회 API와 테스트용 웹 화면을 함께 제공합니다. 데이터 정제 연습이나 실제 개인 금융정보 처리가 목적이 아닙니다.

## 구성과 범위
서버는 시작할 때 JSON을 읽고 메모리에 보관합니다. 요청마다 외부 데이터셋을 다운로드하거나 무작위 거래를 생성하지 않습니다. 파일을 바꾸면 서버를 재시작해야 반영됩니다.

인증, 사용자별 데이터 구분, 거래 추가·수정·삭제, 데이터베이스, 페이지네이션은 제공하지 않습니다. 현재 데이터 규모는 873건입니다.

## 조회

```http
GET /transactions?startDate=2026-01-01&endDate=2026-12-31&category=food&type=EXPENSE
```

지원 필터는 `date`, `startDate`, `endDate`, `description`, `category`, `type`입니다. 모든 필터는 조합할 수 있습니다. `date`는 정확히 하루를 조회하며 기간 필터(`startDate`, `endDate`)와 함께 사용할 수 없습니다.

날짜는 반드시 `YYYY-MM-DD` 형식이어야 합니다. 기간의 양 끝은 포함됩니다. `startDate`만 지정하면 지정한 시작일에서 서버 오늘까지, `endDate`만 지정하면 데이터 최솟값(`2024-01-02`)부터 지정한 종료일까지 조회합니다. 시작일이 종료일보다 늦거나 날짜·열거형 값이 잘못되면 `400`을 반환합니다. 조건에 맞는 거래가 없으면 오류가 아니라 `200`과 `{"count": 0, "transactions": []}`를 반환합니다.

응답 예:

```json
{
  "count": 1,
  "transactions": [
    {
      "transactionId": "txn-000001",
      "date": "2024-01-02",
      "description": "한빛식당",
      "category": "food",
      "amount": 59100,
      "type": "EXPENSE"
    }
  ]
}
```

`category` 허용 값은 `food`, `transport`, `shopping`, `housing_utilities`, `health`, `leisure`, `education`, `other_expense`, `salary`, `side_income`, `investment_income`, `other_income`입니다. `type` 허용 값은 `INCOME`, `EXPENSE`입니다. `description`은 대소문자를 구분하지 않고 부분 일치합니다.

## 데이터와 출처

Kaggle의 [Personal Finance Data](https://www.kaggle.com/datasets/ramyapintchy/personal-finance-data)를 사용합니다. 데이터셋 페이지 기준 작성자는 `Ramya Pintchy`이며, 2026-09-18에 확인한 버전 1의 라이선스 표시는 MIT입니다. 원본 CSV의 2020~2022년 거래 873건을 연도 +4년으로 이동하고, 한국형 설명·카테고리·원화 금액으로 결정적으로 변환합니다. 결과 범위는 2024-01-02~2026-12-31이며 `INCOME` 238건, `EXPENSE` 635건입니다.

Kaggle 페이지에 표시된 MIT 라이선스 라벨을 바탕으로, 데이터셋에만 적용되는 표준 MIT 고지를 아래에 적습니다. Kaggle 페이지가 제공하는 라벨을 안내하는 것이며, 원본 페이지에 아래 문구가 그대로 첨부되어 있었다고 주장하는 것은 아닙니다. 데이터셋 저작권자는 `Ramya Pintchy`로 표기하며, 저작권 연도는 명시하지 않습니다. 이 고지는 이 프로젝트 전체를 별도의 MIT 라이선스로 지정한다는 뜻이 아닙니다.

> Copyright (c) Ramya Pintchy
>
> Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.