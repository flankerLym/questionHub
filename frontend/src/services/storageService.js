const api = '/api/archive'

async function getData() {
  const r = await fetch(api)
  return await r.json()
}
async function saveData(data) {
  await fetch(api, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data)})
}

export const storageService = {
  async init(){ await getData() },
  async getFolders(){ return (await getData()).folders || [] },
  async getAllQuestions(){ return (await getData()).qaItems || [] },
  async getQuestions(folderId){ return (await getData()).qaItems.filter(x=>x.folderId===folderId) },
  async putFolder(folder){
    const d=await getData(); d.folders=d.folders||[];
    const i=d.folders.findIndex(x=>x.id===folder.id);
    if(i>=0)d.folders[i]=folder; else d.folders.push(folder);
    await saveData(d); return folder;
  },
  async deleteFolderCascade(id){
    const d=await getData();
    d.folders=d.folders.filter(x=>x.id!==id);
    d.qaItems=d.qaItems.filter(x=>x.folderId!==id);
    await saveData(d);
  },
  async putQuestion(item){
    const d=await getData(); d.qaItems=d.qaItems||[];
    const i=d.qaItems.findIndex(x=>x.id===item.id);
    if(i>=0)d.qaItems[i]=item; else d.qaItems.push(item);
    await saveData(d); return item;
  },
  async deleteQuestion(id){
    const d=await getData(); d.qaItems=d.qaItems.filter(x=>x.id!==id); await saveData(d);
  },
  async replaceArchive(folders,qaItems){ await saveData({folders,qaItems}); }
}
