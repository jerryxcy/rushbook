# local 與 CI clusters 使用 kind

RushBook 使用 kind 建立可隨時重建的 Kubernetes clusters，供 local
experiments 與 GitHub Actions smoke tests 使用。兩個環境採用相同、已提交
至 Git 的 cluster configuration 與 Helm deployment flow。kind 是測試環境，
不是 production target；managed-cloud deployment 延後處理。
