<template>
  <div>
    <b-form-select
        v-model="selected"
        :options="getSites"
        :placeholder="$t('session.prepare.step.selection.mode.description.withIssueTracker.placeholder.selectSite')"
        @input="getProjects"
    ></b-form-select>
    <div class="mt-3">
      {{ $t("session.prepare.step.selection.mode.description.withIssueTracker.selectedSite") }}
      <strong>{{ selected != null ? selected : "-" }}</strong>
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import apiService from "@/services/api.service";

export default defineComponent({
  name: "SiteSelectionComp",
  data() {
    return {
      selected: '',
    };
  },
  computed: {
    getSites(): Array<string> {
      return this.$store.state.sites;
    }
  },
  methods: {
    async getProjects() {
      console.log(this.selected)
     this.$store.commit("setSelectedSite", this.selected)
      apiService.getAllProjects(this.selected).then((pr) => {
        this.$store.commit("setProjects", pr);
      });
    },
  }


})
</script>


<style scoped>
b-form-select {
  width: 100%;
  border: 1px solid #ccc;
  color: #666;
  border-radius: 10px;
  outline: none;
  padding: 9px 14px;
  box-sizing: border-box;
  font-size: 14px;
}

b-form-select {
  border: 1px solid #ccc;
  max-height: 200px;
  margin-top: 8px;
  width: 100%;
  background-color: white;
  border-radius: 8px;
  overflow: auto;
}

b-form-select {
  padding: 6px 16px;
  color: #4a4a4a;
  max-width: 100%;
  cursor: pointer;
  text-align: left;
  font-size: 14px;
}

b-form-select:hover {
  background-color: #e8e8e8;
}
</style>
